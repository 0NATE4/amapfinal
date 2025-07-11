package com.example.amap.route.util

import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint

/**
 * Utility class for coordinate conversion between AMap SDK types
 */
object AMapServicesUtil {
    
    /**
     * Convert LatLonPoint to LatLng
     */
    fun convertToLatLng(point: LatLonPoint): LatLng {
        return LatLng(point.latitude, point.longitude)
    }
    
    /**
     * Convert LatLng to LatLonPoint
     */
    fun convertToLatLonPoint(latLng: LatLng): LatLonPoint {
        return LatLonPoint(latLng.latitude, latLng.longitude)
    }
    
    /**
     * Convert list of LatLonPoint to list of LatLng
     */
    fun convertArrList(points: List<LatLonPoint>): List<LatLng> {
        return points.map { convertToLatLng(it) }
    }
} 