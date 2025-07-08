package com.example.amap.data.model

import com.amap.api.services.core.PoiItem

// Simple data class for POI display
data class POIDisplayItem(
    val title: String,
    val address: String,
    val poiItem: PoiItem // Keep reference to original for map interaction
) 