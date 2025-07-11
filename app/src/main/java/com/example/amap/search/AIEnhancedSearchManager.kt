package com.example.amap.search

import android.content.Context
import android.location.Location
import android.util.Log
import com.amap.api.services.core.PoiItem
import com.example.amap.ai.AIProcessedQuery
import com.example.amap.ai.DeepSeekAIService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIEnhancedSearchManager(
    private val context: Context,
    private val onSearchResult: (List<PoiItem>?, success: Boolean, message: String, aiInfo: AIProcessedQuery?) -> Unit,
    private val onAIProcessing: (isProcessing: Boolean) -> Unit
) {
    
    private val aiService = DeepSeekAIService()
    private val poiSearchManager = POISearchManager(context) { poiItems, success, message ->
        // This will be called after AI processing and POI search
        onSearchResult(poiItems, success, message, null)
    }
    
    private val searchScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * Enhanced search that uses AI to process the query before searching AMap
     */
    fun performAISearch(userQuery: String, userLocation: Location?) {
        Log.d("AISearch", "Starting AI search for: '$userQuery'")
        Log.d("AISearch", "User location: ${userLocation?.latitude}, ${userLocation?.longitude}")
        
        if (userQuery.trim().isEmpty()) {
            Log.d("AISearch", "Empty query, returning early")
            onSearchResult(null, false, "Please enter a search query", null)
            return
        }
        
        searchScope.launch {
            try {
                // Show AI processing indicator
                onAIProcessing(true)
                Log.d("AISearch", "AI processing started")
                
                // Process query with AI
                val aiProcessedQuery = aiService.processSearchQuery(userQuery, userLocation)
                
                Log.d("AISearch", "AI processed query: $aiProcessedQuery")
                Log.d("AISearch", "Search keywords: ${aiProcessedQuery.searchKeywords}")
                
                // Perform search with the best keyword
                val primaryKeyword = aiProcessedQuery.searchKeywords.firstOrNull() ?: userQuery
                Log.d("AISearch", "Using primary keyword: $primaryKeyword")
                performMultiKeywordSearch(aiProcessedQuery, userLocation)
                
            } catch (e: Exception) {
                Log.e("AISearch", "Error in AI search: ${e.message}", e)
                // Fallback to direct search
                Log.d("AISearch", "Falling back to direct search")
                poiSearchManager.performKeywordSearch(userQuery, userLocation)
                onAIProcessing(false)
            }
        }
    }
    
    /**
     * Perform search with multiple keywords to get better results
     */
    private suspend fun performMultiKeywordSearch(aiQuery: AIProcessedQuery, userLocation: Location?) {
        try {
            Log.d("AISearch", "Starting multi-keyword search with: ${aiQuery.searchKeywords}")
            val allResults = mutableListOf<PoiItem>()
            var successCount = 0
            
            // Search with each keyword (AI already provides them in good order)
            for (keyword in aiQuery.searchKeywords.take(5)) { // Search with top 5 keywords
                Log.d("AISearch", "Searching with keyword: '$keyword'")
                val results = performSingleKeywordSearch(keyword, userLocation)
                Log.d("AISearch", "Found ${results.size} results for keyword '$keyword'")
                if (results.isNotEmpty()) {
                    allResults.addAll(results)
                    successCount++
                }
            }
            
            Log.d("AISearch", "Total results before deduplication: ${allResults.size}")
            
            // Remove duplicates based on POI ID
            val uniqueResults = allResults.distinctBy { it.poiId }
            Log.d("AISearch", "Unique results after deduplication: ${uniqueResults.size}")
            
            // Sort by relevance using intelligent ranking
            val sortedResults = uniqueResults.sortedBy { poi ->
                val poiTitle = poi.title ?: ""
                val poiSnippet = poi.snippet ?: ""
                val fullText = "$poiTitle $poiSnippet"
                
                // Calculate relevance score
                var score = 0
                
                // Exact matches get highest priority
                aiQuery.searchKeywords.forEachIndexed { index, keyword ->
                    if (fullText.contains(keyword, ignoreCase = true)) {
                        score += (10 - index) // Earlier keywords get higher scores
                    }
                }
                
                // Title matches get bonus points
                aiQuery.searchKeywords.forEachIndexed { index, keyword ->
                    if (poiTitle.contains(keyword, ignoreCase = true)) {
                        score += (5 - index)
                    }
                }
                
                // Reverse score so lower numbers = higher priority
                -score
            }
            
            val message = if (successCount > 0) {
                "Found ${sortedResults.size} results using AI-enhanced search"
            } else {
                "No results found with AI-enhanced search"
            }
            
            Log.d("AISearch", "Final message: $message")
            Log.d("AISearch", "Final result count: ${sortedResults.size}")
            
            onSearchResult(
                sortedResults.take(20), // Limit results
                sortedResults.isNotEmpty(),
                message,
                aiQuery
            )
            
        } catch (e: Exception) {
            Log.e("AISearch", "Error in multi-keyword search: ${e.message}", e)
            onSearchResult(null, false, "Search failed", aiQuery)
        } finally {
            onAIProcessing(false)
        }
    }
    
    /**
     * Perform a single keyword search and return results
     */
    private suspend fun performSingleKeywordSearch(keyword: String, userLocation: Location?): List<PoiItem> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<PoiItem>()
                var searchCompleted = false
                var searchResults: List<PoiItem>? = null
                var searchSuccess = false
                var searchMessage = ""
                
                // Create a temporary callback for this search
                val tempCallback: (List<PoiItem>?, Boolean, String) -> Unit = { poiItems, success, message ->
                    searchResults = poiItems
                    searchSuccess = success
                    searchMessage = message
                    searchCompleted = true
                }
                
                // Create temporary search manager
                val tempSearchManager = POISearchManager(context, tempCallback)
                tempSearchManager.performKeywordSearch(keyword, userLocation)
                
                // Wait for search to complete (with timeout)
                var attempts = 0
                while (!searchCompleted && attempts < 50) { // 5 second timeout
                    kotlinx.coroutines.delay(100)
                    attempts++
                }
                
                if (searchSuccess && searchResults != null) {
                    results.addAll(searchResults!!)
                }
                
                tempSearchManager.cleanup()
                results
                
            } catch (e: Exception) {
                Log.e("AISearch", "Error in single keyword search for '$keyword': ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * Fallback to direct search without AI processing
     */
    fun performDirectSearch(query: String, userLocation: Location?) {
        poiSearchManager.performKeywordSearch(query, userLocation)
    }
    
    fun cleanup() {
        poiSearchManager.cleanup()
    }
} 