package com.example.amap.core

object Constants {
    
    // API Keys
    object ApiKeys {
        // Web Service API key for HTTP requests (place/detail, place/around, etc.)
        const val WEB_API_KEY = "5de47f40dbe4ca643860ca696f8e5d4e"
        // Android SDK key is in AndroidManifest.xml: f7e69e105a57569fc6c5e8d5e36948d7
        
        // DeepSeek AI API Key
        const val DEEPSEEK_API_KEY = "sk-f4ad3811b3c3417c8ad8fea2cdc7cead"
        const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1/"
    }
    
    // Search Configuration
    object Search {
        const val DEFAULT_PAGE_SIZE = 10
        const val DEFAULT_SEARCH_RADIUS = 1000 // meters
    }
    
    // AI Configuration
    object AI {
        const val MAX_TOKENS = 1000
        const val TEMPERATURE = 0.7
        const val TIMEOUT_SECONDS = 30L
    }
    
    // Map Configuration
    object Map {
        const val DEFAULT_ZOOM_LEVEL = 14f
        const val POI_FOCUS_ZOOM_LEVEL = 16f
        const val SEARCH_RESULTS_ZOOM_LEVEL = 15f
        const val LOCATION_UPDATE_INTERVAL = 5000L // milliseconds
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