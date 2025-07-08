package com.example.amap.core

object Constants {
    
    // Search Configuration
    object Search {
        const val DEFAULT_PAGE_SIZE = 10
        const val NEARBY_PAGE_SIZE = 20
        const val DEFAULT_SEARCH_RADIUS = 1000 // meters
        const val NEARBY_SEARCH_RADIUS = 500 // meters
    }
    
    // Map Configuration
    object Map {
        const val DEFAULT_ZOOM_LEVEL = 14f
        const val POI_FOCUS_ZOOM_LEVEL = 16f
        const val SEARCH_RESULTS_ZOOM_LEVEL = 15f
        const val LOCATION_UPDATE_INTERVAL = 2000L // milliseconds
    }
    
    // Search Result Codes
    object SearchCodes {
        const val SUCCESS = 1000
    }
    
    // Distance Calculation
    object Distance {
        const val EARTH_RADIUS_METERS = 6371000.0
    }
} 