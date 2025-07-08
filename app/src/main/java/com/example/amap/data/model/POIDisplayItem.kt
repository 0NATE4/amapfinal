package com.example.amap.data.model

import com.amap.api.services.core.PoiItem

// Simple data class for POI display
data class POIDisplayItem(
    val title: String,
    val address: String,
    val distance: String, // Distance from user location (e.g. "1.2km")
    val poiItem: PoiItem // Keep reference to original for map interaction
) 