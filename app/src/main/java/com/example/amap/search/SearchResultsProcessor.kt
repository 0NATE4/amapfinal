package com.example.amap.search

import android.location.Location
import com.amap.api.services.core.PoiItem
import com.example.amap.core.Constants
import com.example.amap.data.model.POIDisplayItem
import com.example.amap.util.PinyinUtil

class SearchResultsProcessor {
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

            POIDisplayItem(
                title = poi.title ?: "Unknown POI",
                address = address,
                distance = distance,
                poiItem = poi,
                englishTitle = PinyinUtil.toPinyin(poi.title ?: "Unknown POI"),
                englishAddress = PinyinUtil.toPinyin(address)
            )
        }
    }

    // Remove processResultsWithTranslations and AI translation logic

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