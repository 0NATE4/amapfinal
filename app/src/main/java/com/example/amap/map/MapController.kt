package com.example.amap.map

import android.location.Location
import android.util.Log
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.core.PoiItem
import com.example.amap.core.Constants

class MapController(private val aMap: AMap) {

    private val poiMarkers = mutableListOf<Marker>()

    fun addPOIMarkers(poiItems: List<PoiItem>, userLocation: Location?): List<Marker> {
        val newMarkers = mutableListOf<Marker>()
        
        for (poi in poiItems) {
            val latLng = LatLng(poi.latLonPoint.latitude, poi.latLonPoint.longitude)
            
            val snippet = if (userLocation != null) {
                val distance = calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    poi.latLonPoint.latitude, poi.latLonPoint.longitude
                )
                "${poi.snippet ?: poi.adName} • ${distance}m away"
            } else {
                poi.snippet ?: poi.adName
            }
            
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(poi.title ?: "Unknown POI")
                    .snippet(snippet)
            )
            
            newMarkers.add(marker)
            poiMarkers.add(marker)
            
            Log.d("MapController", "Added marker: ${poi.title} at ${poi.latLonPoint.latitude}, ${poi.latLonPoint.longitude}")
        }
        
        return newMarkers
    }

    fun clearPOIMarkers() {
        for (marker in poiMarkers) {
            marker.remove()
        }
        poiMarkers.clear()
    }

    fun focusOnPOI(poiItem: PoiItem) {
        val latLng = LatLng(poiItem.latLonPoint.latitude, poiItem.latLonPoint.longitude)
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.Map.POI_FOCUS_ZOOM_LEVEL))
        
        // Find and show info window for this POI
        for (marker in poiMarkers) {
            if (marker.position.latitude == latLng.latitude && 
                marker.position.longitude == latLng.longitude) {
                marker.showInfoWindow()
                break
            }
        }
    }

    fun centerOnUserLocation(): Boolean {
        val userLocation = aMap.myLocation
        return if (userLocation != null) {
            val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, Constants.Map.DEFAULT_ZOOM_LEVEL))
            true
        } else {
            false
        }
    }

    fun centerOnResults(poiItems: List<PoiItem>, userLocation: Location?) {
        if (poiItems.isEmpty()) return
        
        if (userLocation != null) {
            // Center between user and POIs
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(userLocation.latitude, userLocation.longitude), Constants.Map.DEFAULT_ZOOM_LEVEL))
        } else {
            // Center on first POI
            val firstPoi = poiItems[0]
            val firstLatLng = LatLng(firstPoi.latLonPoint.latitude, firstPoi.latLonPoint.longitude)
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng, Constants.Map.SEARCH_RESULTS_ZOOM_LEVEL))
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