package com.example.amap.route.overlay

import android.content.Context
import android.util.Log
import com.amap.api.maps.AMap
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.services.core.LatLonPoint
import com.example.amap.RouteController
import com.example.amap.route.util.AMapServicesUtil

/**
 * Walking route overlay for displaying route from Web Service API
 * Adapted from official AMap WalkRouteOverlay.java
 */
class WalkRouteOverlay(
    context: Context,
    private val map: AMap,
    private val routeData: RouteController.RouteData,
    private val start: LatLonPoint,
    private val end: LatLonPoint
) : RouteOverlay(context) {

    init {
        this.aMap = map
        this.startPoint = AMapServicesUtil.convertToLatLng(start)
        this.endPoint = AMapServicesUtil.convertToLatLng(end)
    }

    /**
     * Add the walking route to the map
     */
    fun addToMap() {
        try {
            // Create polyline options
            val polylineOptions = PolylineOptions()
                .color(getWalkColor())
                .width(getRouteWidth())

            // Add start point
            startPoint?.let { polylineOptions.add(it) }

            // Parse and add polyline points from route data
            val routePoints = parsePolylineString(routeData.polyline)
            if (routePoints.isNotEmpty()) {
                polylineOptions.addAll(routePoints)
                
                // Add some intermediate markers for demonstration
                addIntermediateMarkers(routePoints)
            }

            // Add end point
            endPoint?.let { polylineOptions.add(it) }

            // Add start and end markers
            addStartAndEndMarker()

            // Show the polyline
            addPolyLine(polylineOptions)

            // Zoom to show the entire route
            zoomToSpan()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Parse AMap polyline string format into LatLng points
     * Can handle both single polylines and multiple polylines joined by ";;"
     * AMap polyline format: "lng1,lat1;lng2,lat2;lng3,lat3" or multiple polylines
     */
    private fun parsePolylineString(polyline: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        
        try {
            if (polyline.isNotEmpty()) {
                // Handle multiple polylines from different steps (separated by ";")
                val polylineSegments = polyline.split(";;") // In case there are double separators
                
                for (segment in polylineSegments) {
                    if (segment.isNotEmpty()) {
                        val coordPairs = segment.split(";")
                        for (pair in coordPairs) {
                            if (pair.isNotEmpty()) {
                                val coords = pair.split(",")
                                if (coords.size >= 2) {
                                    val lng = coords[0].trim().toDoubleOrNull()
                                    val lat = coords[1].trim().toDoubleOrNull()
                                    if (lng != null && lat != null) {
                                        points.add(LatLng(lat, lng))
                                    }
                                }
                            }
                        }
                    }
                }
                
                Log.d("WalkRouteOverlay", "Parsed ${points.size} route points from polyline")
                if (points.size < 2) {
                    Log.w("WalkRouteOverlay", "Warning: Very few route points parsed, route may look incomplete")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WalkRouteOverlay", "Error parsing polyline: ${e.message}")
            // Fallback: just connect start and end points directly
            startPoint?.let { start ->
                endPoint?.let { end ->
                    points.add(start)
                    points.add(end)
                    Log.d("WalkRouteOverlay", "Using fallback direct route")
                }
            }
        }
        
        return points
    }

    /**
     * Add some intermediate markers along the route
     */
    private fun addIntermediateMarkers(routePoints: List<LatLng>) {
        try {
            // Add markers more sparsely - every 20 points or at least every 200m
            val stepSize = maxOf(20, routePoints.size / 5) // Ensure we don't have too many markers
            
            for (i in routePoints.indices step stepSize) {
                if (i > 0 && i < routePoints.size - 1) { // Skip start and end
                    val point = routePoints[i]
                    addStationMarker(
                        MarkerOptions()
                            .position(point)
                            .title("Walking waypoint")
                            .snippet("Continue ahead")
                            .visible(nodeIconVisible)
                            .anchor(0.5f, 0.5f)
                            .icon(getWalkBitmapDescriptor())
                    )
                }
            }
            
            Log.d("WalkRouteOverlay", "Added ${(routePoints.size / stepSize)} intermediate markers")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 