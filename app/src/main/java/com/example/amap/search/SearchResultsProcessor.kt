package com.example.amap.search

import android.location.Location
import com.amap.api.services.core.PoiItem
import com.example.amap.core.Constants
import com.example.amap.data.model.POIDisplayItem
import com.example.amap.ai.DeepSeekAIService
import android.util.Log
import kotlinx.coroutines.runBlocking

class SearchResultsProcessor {
    private val aiService = DeepSeekAIService()

    fun processResults(poiItems: List<PoiItem>, userLocation: Location?): List<POIDisplayItem> {
        return runBlocking {
            processResultsWithTranslation(poiItems, userLocation)
        }
    }

    private suspend fun processResultsWithTranslation(poiItems: List<PoiItem>, userLocation: Location?): List<POIDisplayItem> {
        Log.d("SearchProcessor", "Processing ${poiItems.size} POI results with AI translation")
        
        // Extract business names for batch translation
        val businessNames = poiItems.map { poi ->
            poi.title ?: "Unknown POI"
        }
        
        // Translate business names in batch (more efficient than individual calls)
        val translatedNames = try {
            aiService.translateBusinessNames(businessNames)
        } catch (e: Exception) {
            Log.e("SearchProcessor", "Translation failed, using original names: ${e.message}")
            businessNames // Fallback to original names
        }
        
        Log.d("SearchProcessor", "Translated ${translatedNames.size} business names")
        
        return poiItems.mapIndexed { index, poi ->
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

            val chineseName = businessNames[index]
            val englishName = if (index < translatedNames.size) translatedNames[index] else chineseName

            POIDisplayItem(
                title = chineseName, // Keep Chinese as primary title
                address = address, // Keep Chinese address
                distance = distance,
                poiItem = poi,
                englishTitle = if (englishName != chineseName) englishName else null, // Only set if different
                englishAddress = null // Don't translate addresses as per requirements
            )
        }
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

    private fun formatDistance(meters: Int): String {
        return if (meters < 1000) {
            "${meters}m"
        } else {
            val km = meters / 1000.0
            "%.1fkm".format(km)
        }
    }
} 