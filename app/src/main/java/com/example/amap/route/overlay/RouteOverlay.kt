package com.example.amap.route.overlay

import android.content.Context
import android.graphics.Color
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*

/**
 * Base class for route overlays
 * Converted from official AMap RouteOverlay.java
 */
open class RouteOverlay(protected val context: Context) {
    
    protected val stationMarkers = mutableListOf<Marker>()
    protected val allPolyLines = mutableListOf<Polyline>()
    protected var startMarker: Marker? = null
    protected var endMarker: Marker? = null
    protected var startPoint: LatLng? = null
    protected var endPoint: LatLng? = null
    protected lateinit var aMap: AMap
    protected var nodeIconVisible = true

    /**
     * Remove all markers and polylines from the map
     */
    fun removeFromMap() {
        startMarker?.remove()
        endMarker?.remove()
        
        stationMarkers.forEach { it.remove() }
        stationMarkers.clear()
        
        allPolyLines.forEach { it.remove() }
        allPolyLines.clear()
    }

    /**
     * Get start marker icon - using simple colored circle
     */
    protected open fun getStartBitmapDescriptor(): BitmapDescriptor {
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
    }

    /**
     * Get end marker icon - using simple colored circle  
     */
    protected open fun getEndBitmapDescriptor(): BitmapDescriptor {
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    }

    /**
     * Get walking step marker icon - using simple colored circle
     */
    protected open fun getWalkBitmapDescriptor(): BitmapDescriptor {
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
    }

    /**
     * Add start and end markers to the map
     */
    protected fun addStartAndEndMarker() {
        startPoint?.let { start ->
            startMarker = aMap.addMarker(
                MarkerOptions()
                    .position(start)
                    .icon(getStartBitmapDescriptor())
                    .title("起点")
            )
        }

        endPoint?.let { end ->
            endMarker = aMap.addMarker(
                MarkerOptions()
                    .position(end)
                    .icon(getEndBitmapDescriptor())
                    .title("终点")
            )
        }
    }

    /**
     * Zoom camera to show the entire route
     */
    fun zoomToSpan() {
        startPoint?.let {
            try {
                val bounds = getLatLngBounds()
                aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Get bounds that include all route points
     */
    protected fun getLatLngBounds(): LatLngBounds {
        val builder = LatLngBounds.builder()
        
        startPoint?.let { builder.include(it) }
        endPoint?.let { builder.include(it) }
        
        allPolyLines.forEach { polyline ->
            polyline.points.forEach { point ->
                builder.include(point)
            }
        }
        
        return builder.build()
    }

    /**
     * Control visibility of route node icons
     */
    fun setNodeIconVisibility(visible: Boolean) {
        try {
            nodeIconVisible = visible
            stationMarkers.forEach { it.isVisible = visible }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Add a station marker to the map
     */
    protected fun addStationMarker(options: MarkerOptions?) {
        options?.let {
            val marker = aMap.addMarker(it)
            marker?.let { m ->
                stationMarkers.add(m)
            }
        }
    }

    /**
     * Add a polyline to the map
     */
    protected fun addPolyLine(options: PolylineOptions?) {
        options?.let {
            val polyline = aMap.addPolyline(it)
            polyline?.let { p ->
                allPolyLines.add(p)
            }
        }
    }

    /**
     * Get route line width
     */
    protected open fun getRouteWidth(): Float = 18f

    /**
     * Get walking route color
     */
    protected open fun getWalkColor(): Int = Color.parseColor("#6db74d")
} 