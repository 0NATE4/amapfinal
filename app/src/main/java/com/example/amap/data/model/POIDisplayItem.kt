package com.example.amap.data.model

import com.amap.api.services.core.PoiItem

// Simple data class for POI display
data class POIDisplayItem(
    val title: String,
    val address: String,
    val distance: String, // Distance from user location (e.g. "1.2km")
    val poiItem: PoiItem, // Keep reference to original for map interaction
    
    // English translations for foreigners
    val englishTitle: String? = null,
    val englishAddress: String? = null,
    
    // Rich details from Web API (null until fetched)
    val richDetails: POIRichDetails? = null
)

// Rich POI details from Amap Web Service API
data class POIRichDetails(
    val id: String,
    val photos: List<POIPhoto>,
    val rating: String?,
    val cost: String?,
    val openHours: String?,
    val telephone: String?,
    val businessArea: String?,
    val tags: List<String>,
    val reviews: List<POIReview>? = null
)

data class POIPhoto(
    val title: String?,
    val url: String
)

data class POIReview(
    val content: String,
    val rating: String?,
    val author: String?
) 