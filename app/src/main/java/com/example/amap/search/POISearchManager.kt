package com.example.amap.search

import android.content.Context
import android.location.Location
import android.util.Log
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.example.amap.core.Constants

class POISearchManager(
    private val context: Context,
    private val onSearchResult: (List<PoiItem>?, success: Boolean, message: String) -> Unit
) : PoiSearch.OnPoiSearchListener {

    private var poiSearch: PoiSearch? = null

    fun performKeywordSearch(keyword: String, userLocation: Location?) {
        Log.d("POISearch", "Starting search for: $keyword")
        
        val query = PoiSearch.Query(keyword, "", "")
        query.pageSize = Constants.Search.DEFAULT_PAGE_SIZE
        query.pageNum = 1
        
        poiSearch = PoiSearch(context, query)
        poiSearch?.setOnPoiSearchListener(this)
        
        if (userLocation != null) {
            val userLatLonPoint = LatLonPoint(userLocation.latitude, userLocation.longitude)
            val searchBound = PoiSearch.SearchBound(userLatLonPoint, Constants.Search.DEFAULT_SEARCH_RADIUS)
            poiSearch?.bound = searchBound
            
            Log.d("POISearch", "Using nearby search around user location: ${userLocation.latitude}, ${userLocation.longitude}")
        } else {
            Log.d("POISearch", "User location not available, using general search")
        }
        
        poiSearch?.searchPOIAsyn()
    }



    override fun onPoiSearched(result: PoiResult?, rCode: Int) {
        Log.d("POISearch", "Search result received. Code: $rCode")
        
        if (rCode == Constants.SearchCodes.SUCCESS && result != null) {
            val poiItems = result.pois
            if (poiItems != null && poiItems.isNotEmpty()) {
                Log.d("POISearch", "Found ${poiItems.size} POIs")
                onSearchResult(poiItems, true, "Found ${poiItems.size} results")
            } else {
                Log.d("POISearch", "No POI results found")
                onSearchResult(null, false, "No results found")
            }
        } else {
            Log.e("POISearch", "Search failed with code: $rCode")
            onSearchResult(null, false, "Search failed")
        }
    }

    override fun onPoiItemSearched(item: PoiItem?, rCode: Int) {
        // Not used for basic keyword search
    }

    fun cleanup() {
        poiSearch?.setOnPoiSearchListener(null)
        poiSearch = null
    }
} 