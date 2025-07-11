package com.example.amap.search

import android.location.Location
import com.amap.api.services.core.PoiItem
import com.example.amap.ai.DeepSeekAIService
import com.example.amap.ai.POITranslationItem
import com.example.amap.core.Constants
import com.example.amap.data.model.POIDisplayItem
import com.example.amap.util.PinyinUtil
import android.util.Log

class SearchResultsProcessor {
    private val aiService = DeepSeekAIService()
    
    // Translation cache to store batch translations for titles only
    private val translationCache = mutableMapOf<String, String>() // POI ID -> englishTitle
    
    fun processResults(poiItems: List<PoiItem>, userLocation: Location?): List<POIDisplayItem> {
        return poiItems.map { poi ->
            val address = poi.snippet ?: poi.adName ?: "Unknown address"
            val distance = if (userLocation != null) {
                val distanceMeters = calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    poi.latLonPoint.latitude, poi.latLonPoint.longitude
                )
                formatDistance(distanceMeters)
            } else {
                "N/A"
            }

            // Check if we have cached translation for this POI title
            val poiKey = generatePOIKey(poi)
            val englishTitle = translationCache[poiKey] ?: PinyinUtil.toPinyin(poi.title ?: "Unknown POI")

            POIDisplayItem(
                title = poi.title ?: "Unknown POI",
                address = address,
                distance = distance,
                poiItem = poi,
                englishTitle = englishTitle
            )
        }
    }

    /**
     * Process results with batch AI translation for English titles only
     */
    suspend fun processResultsWithBatchTranslation(poiItems: List<PoiItem>, userLocation: Location?): List<POIDisplayItem> {
        // First create basic POI display items
        val basicItems = processResults(poiItems, userLocation)
        
        // Prepare batch translation data for titles only
        val translationItems = basicItems.map { poi ->
            POITranslationItem(
                id = "${poi.poiItem.poiId ?: poi.title}_${System.currentTimeMillis()}", // Generate unique ID
                originalTitle = poi.title
            )
        }
        
        // Perform batch translation
        val translations = aiService.batchTranslatePOIs(translationItems)
        
        // Create a map for quick lookup
        val translationMap = translations.associateBy { it.id }
        
        // Apply translations to POI display items
        return basicItems.mapIndexed { index, poi ->
            val translationItem = translationItems[index]
            val translation = translationMap[translationItem.id]
            
            poi.copy(
                englishTitle = translation?.translatedTitle ?: poi.englishTitle
            )
        }
    }
    
    /**
     * Cache translations for future use - titles only
     */
    fun cacheTranslations(translatedItems: List<POIDisplayItem>) {
        translatedItems.forEach { item ->
            val poiKey = generatePOIKey(item.poiItem)
            // Only cache non-null titles
            val englishTitle = item.englishTitle ?: "Unknown POI"
            translationCache[poiKey] = englishTitle
        }
        Log.d("SearchResultsProcessor", "Cached title translations for ${translatedItems.size} POIs")
    }
    
    /**
     * Clear the translation cache
     */
    fun clearTranslationCache() {
        translationCache.clear()
        Log.d("SearchResultsProcessor", "Translation cache cleared")
    }
    
    /**
     * Generate a unique key for a POI item for caching
     */
    private fun generatePOIKey(poi: PoiItem): String {
        // Use POI ID if available, otherwise create a key from title and coordinates
        return poi.poiId ?: "${poi.title}_${poi.latLonPoint.latitude}_${poi.latLonPoint.longitude}"
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (Constants.Distance.EARTH_RADIUS_METERS * c).toInt()
    }
    
    private fun formatDistance(distanceMeters: Int): String {
        return when {
            distanceMeters < 1000 -> "${distanceMeters}m"
            else -> "${"%.1f".format(distanceMeters / 1000.0)}km"
        }
    }
} 