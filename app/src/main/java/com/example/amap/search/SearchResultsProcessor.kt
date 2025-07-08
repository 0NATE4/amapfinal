package com.example.amap.search

import android.location.Location
import com.amap.api.services.core.PoiItem
import com.example.amap.core.Constants
import com.example.amap.data.model.POIDisplayItem

class SearchResultsProcessor {

    fun processResults(poiItems: List<PoiItem>, userLocation: Location?): List<POIDisplayItem> {
        return poiItems.map { poi ->
            val snippet = if (userLocation != null) {
                val distance = calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    poi.latLonPoint.latitude, poi.latLonPoint.longitude
                )
                "${poi.snippet ?: poi.adName} • ${distance}m away"
            } else {
                poi.snippet ?: poi.adName
            }

            POIDisplayItem(
                title = poi.title ?: "Unknown POI",
                address = snippet,
                poiItem = poi
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
} 