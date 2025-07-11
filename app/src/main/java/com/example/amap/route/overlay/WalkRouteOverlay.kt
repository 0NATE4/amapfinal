package com.example.amap.route.overlay

import android.content.Context
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
     * AMap polyline format: "lng1,lat1;lng2,lat2;lng3,lat3"
     */
    private fun parsePolylineString(polyline: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        
        try {
            if (polyline.isNotEmpty()) {
                val coordPairs = polyline.split(";")
                for (pair in coordPairs) {
                    val coords = pair.split(",")
                    if (coords.size >= 2) {
                        val lng = coords[0].toDoubleOrNull()
                        val lat = coords[1].toDoubleOrNull()
                        if (lng != null && lat != null) {
                            points.add(LatLng(lat, lng))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: just connect start and end points directly
            startPoint?.let { start ->
                endPoint?.let { end ->
                    points.add(start)
                    points.add(end)
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
            // Add a marker every 5 points to show route progress
            for (i in routePoints.indices step 5) {
                if (i > 0 && i < routePoints.size - 1) { // Skip start and end
                    val point = routePoints[i]
                    addStationMarker(
                        MarkerOptions()
                            .position(point)
                            .title("步行点 ${i / 5 + 1}")
                            .snippet("继续前行")
                            .visible(nodeIconVisible)
                            .anchor(0.5f, 0.5f)
                            .icon(getWalkBitmapDescriptor())
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 