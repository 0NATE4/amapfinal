package com.example.amap.map

import android.content.Context
import android.location.Location
import android.util.Log
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.core.PoiItem
import com.example.amap.R
import com.example.amap.RouteController
import com.example.amap.core.Constants
import com.example.amap.route.overlay.WalkRouteOverlay

class MapController(private val aMap: AMap, private val context: Context) {

    private val poiMarkers = mutableListOf<Marker>()
    private var currentRouteOverlay: WalkRouteOverlay? = null
    private val customInfoWindowAdapter = CustomInfoWindowAdapter(context)
    
    init {
        // Set custom info window adapter
        aMap.setInfoWindowAdapter(customInfoWindowAdapter)
    }

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
            
            // Get custom marker icon based on POI category
            val markerIcon = getCustomMarkerIcon(poi)
            
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(poi.title ?: "Unknown POI")
                    .snippet(snippet)
                    .icon(markerIcon)
                    .anchor(0.5f, 1.0f) // Center horizontally, bottom anchored
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
        
        // Animate camera to POI with smooth transition
        aMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, Constants.Map.POI_FOCUS_ZOOM_LEVEL),
            1000,  // 1 second animation
            object : AMap.CancelableCallback {
                override fun onFinish() {
                    // Show info window after animation completes
                    showInfoWindowForPOI(latLng)
                }
                override fun onCancel() {
                    // Still try to show info window if animation was cancelled
                    showInfoWindowForPOI(latLng)
                }
            }
        )
    }
    
    /**
     * Show info window for POI at given location
     */
    private fun showInfoWindowForPOI(latLng: LatLng) {
        // Find and show info window for this POI
        for (marker in poiMarkers) {
            // Check if marker is at the same location (with small tolerance for floating point comparison)
            val distance = calculateDistance(
                marker.position.latitude, marker.position.longitude,
                latLng.latitude, latLng.longitude
            )
            if (distance < 10) { // Within 10 meters
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

    /**
     * Show a walking route overlay on the map
     */
    fun showWalkingRoute(routeResult: RouteController.RouteResult) {
        // Clear any existing route
        clearCurrentRoute()
        
        try {
            // Create and display the walking route overlay
            currentRouteOverlay = WalkRouteOverlay(
                context = context,
                map = aMap,
                routeData = routeResult.routeData,
                start = routeResult.startPoint,
                end = routeResult.endPoint
            )
            
            currentRouteOverlay?.addToMap()
            Log.d("MapController", "Walking route overlay added to map")
            
        } catch (e: Exception) {
            Log.e("MapController", "Error showing walking route", e)
        }
    }
    
    /**
     * Clear the current route overlay from the map
     */
    fun clearCurrentRoute() {
        currentRouteOverlay?.removeFromMap()
        currentRouteOverlay = null
        Log.d("MapController", "Route overlay cleared")
    }
    
    /**
     * Get custom marker icon based on POI category
     */
    private fun getCustomMarkerIcon(poi: PoiItem) = 
        BitmapDescriptorFactory.fromResource(getCategoryMarkerResource(poi))
    
    /**
     * Determine marker icon resource based on POI category
     */
    private fun getCategoryMarkerResource(poi: PoiItem): Int {
        val title = poi.title ?: ""
        val typeDes = poi.typeDes ?: ""
        val category = "$title $typeDes".lowercase()
        
        return when {
            category.contains("餐") || category.contains("restaurant") || 
            category.contains("food") || category.contains("咖啡") ||
            category.contains("coffee") || category.contains("饭店") ||
            category.contains("麦当劳") || category.contains("肯德基") -> 
                R.drawable.ic_poi_pin_restaurant
            
            category.contains("购物") || category.contains("shopping") || 
            category.contains("mall") || category.contains("商场") ||
            category.contains("超市") || category.contains("market") ||
            category.contains("店") -> 
                R.drawable.ic_poi_pin_shopping
            
            category.contains("娱乐") || category.contains("entertainment") || 
            category.contains("cinema") || category.contains("电影") ||
            category.contains("ktv") || category.contains("游戏") ||
            category.contains("酒吧") -> 
                R.drawable.ic_poi_pin_entertainment
            
            category.contains("交通") || category.contains("transport") || 
            category.contains("station") || category.contains("地铁") ||
            category.contains("公交") || category.contains("火车") ||
            category.contains("机场") || category.contains("airport") ||
            category.contains("停车") -> 
                R.drawable.ic_poi_pin_transport
            
            category.contains("医") || category.contains("health") || 
            category.contains("hospital") || category.contains("诊所") ||
            category.contains("pharmacy") || category.contains("药店") -> 
                R.drawable.ic_poi_pin_health
            
            category.contains("教育") || category.contains("education") || 
            category.contains("school") || category.contains("学校") ||
            category.contains("大学") || category.contains("university") -> 
                R.drawable.ic_poi_pin_education
            
            category.contains("银行") || category.contains("bank") ||
            category.contains("atm") -> 
                R.drawable.ic_poi_pin_banking
            
            category.contains("酒店") || category.contains("hotel") ||
            category.contains("宾馆") -> 
                R.drawable.ic_poi_pin_hotel
            
            else -> R.drawable.ic_poi_pin_default
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