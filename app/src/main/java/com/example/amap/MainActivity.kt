package com.example.amap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.view.View
import android.widget.Toast
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.PoiItem
import com.example.amap.core.Constants
import com.example.amap.data.model.POIDisplayItem
import com.example.amap.map.MapViewModel
import com.example.amap.map.MapController
import com.example.amap.search.POISearchManager
import com.example.amap.search.SearchResultsProcessor
import com.example.amap.search.AIEnhancedSearchManager
import com.example.amap.ai.AIProcessedQuery
import com.example.amap.ui.POIResultsAdapter
import com.example.amap.ui.POIDetailsManager
import com.example.amap.ui.SearchUIHandler
import com.example.amap.util.AmapPrivacy
import com.amap.api.services.core.LatLonPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var aMap: AMap
    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageView
    private lateinit var searchContainer: CardView
    private lateinit var resultsContainer: CardView
    private lateinit var myLocationButton: FloatingActionButton
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var loadingOverlay: View
    private lateinit var aiProcessingIndicator: TextView
    private lateinit var poiAdapter: POIResultsAdapter
    
    // Components
    private lateinit var mapController: MapController
    private lateinit var poiSearchManager: POISearchManager
    private lateinit var aiSearchManager: AIEnhancedSearchManager
    private lateinit var searchResultsProcessor: SearchResultsProcessor
    private lateinit var searchUIHandler: SearchUIHandler
    private lateinit var poiDetailsManager: POIDetailsManager
    private lateinit var routeController: RouteController

    private val viewModel: MapViewModel by viewModels()
    private var mapReadyForDisplay = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.d("Permission", "Permission result received: $isGranted")
            viewModel.onPermissionResult(isGranted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AmapPrivacy.ensureCompliance(this)
        setContentView(R.layout.activity_main)

        initializeViews(savedInstanceState)
        setupBasicUI()
        setupClearButton()
        
        checkInitialPermissions()
        observeUiState()
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        searchEditText = findViewById(R.id.searchEditText)
        clearButton = findViewById(R.id.clearButton)
        searchContainer = findViewById(R.id.searchContainer)
        resultsContainer = findViewById(R.id.resultsContainer)
        myLocationButton = findViewById(R.id.myLocationButton)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        aiProcessingIndicator = findViewById(R.id.aiProcessingIndicator)
        
        mapView = findViewById(R.id.map)
        // Don't create map yet - wait until we're ready to show it
    }

    private fun setupBasicUI() {
        // Setup basic UI that doesn't depend on map
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        poiAdapter = POIResultsAdapter { poiDisplayItem ->
            if (::mapController.isInitialized) {
                // Hide search results list to show map clearly
                hideResultsWithAnimation()
                
                // Focus on POI in map
                focusOnPOI(poiDisplayItem.poiItem)
                
                // Show rich details in bottom sheet
                if (::poiDetailsManager.isInitialized) {
                    poiDetailsManager.showPOIDetails(poiDisplayItem)
                }
            }
        }
        resultsRecyclerView.adapter = poiAdapter
    }

    private fun setupClearButton() {
        clearButton.setOnClickListener {
            searchEditText.text.clear()
            clearSearchResults()
            hideResultsWithAnimation()
            clearButton.visibility = View.GONE
        }
    }
    
    private fun initializeMapComponents() {
        mapController = MapController(aMap, this)
        poiSearchManager = POISearchManager(this) { poiItems, success, message ->
            handleSearchResult(poiItems, success, message)
        }
        
        // Initialize AI-enhanced search manager
        aiSearchManager = AIEnhancedSearchManager(
            context = this,
            onSearchResult = { poiItems, success, message, aiInfo ->
                handleAISearchResult(poiItems, success, message, aiInfo)
            },
            onAIProcessing = { isProcessing ->
                if (isProcessing) {
                    searchUIHandler.showAIProcessing()
                } else {
                    searchUIHandler.hideAIProcessing()
                }
            }
        )
        
        searchResultsProcessor = SearchResultsProcessor()
        
        // Initialize POI details manager for rich data display
        poiDetailsManager = POIDetailsManager(
            context = this, 
            lifecycleScope = lifecycleScope, 
            apiKey = Constants.ApiKeys.WEB_API_KEY,
            onDirectionsRequested = { poiDisplayItem -> planRouteFromDirectionsButton(poiDisplayItem) }
        )
        
        // Initialize route controller for walking route planning
        routeController = RouteController(this) { success, message, result ->
            handleRouteResult(success, message, result)
        }
        
        searchUIHandler = SearchUIHandler(
            searchEditText = searchEditText,
            onSearch = { query -> performAISearch(query) },
            onTextChanged = { text -> 
                clearButton.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
            },
            aiProcessingIndicator = aiProcessingIndicator,
            searchContainer = searchContainer
        )
        
        searchUIHandler.setupSearchListeners()
        
        // Setup my location button
        myLocationButton.setOnClickListener {
            val success = mapController.centerOnUserLocation()
            if (!success) {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
        
        Log.d("MainActivity", "Map components initialized")
    }

    private fun checkInitialPermissions() {
        // This function just checks the initial state and either updates the ViewModel
        // or launches the permission request.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionResult(true)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isLocationPermissionGranted) {
                    Log.d("MainActivity", "State updated: Permission is now granted.")
                    
                    // Initialize map only when we have permission
                    if (!::aMap.isInitialized) {
                        initializeMap()
                    }
                    
                    viewModel.setupLocationStyle(aMap, this@MainActivity)
                    
                    // Wait for actual location before showing map
                    waitForLocationAndShowMap()
                } else {
                    Log.d("MainActivity", "State updated: Permission is not granted.")
                }
            }
        }
    }
    
    private fun initializeMap() {
        mapView.onCreate(null)
        aMap = mapView.map
        
        // Set initial camera to world view to avoid Beijing default
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 2f))
        
        // Now initialize components that depend on the map
        initializeMapComponents()
        
        Log.d("MainActivity", "Map initialized")
    }

    private fun waitForLocationAndShowMap() {
        var attempts = 0
        val maxAttempts = 20 // 10 seconds max
        
        val locationChecker = object : Runnable {
            override fun run() {
                val location = if (::mapController.isInitialized) mapController.getUserLocation() else null
                attempts++
                
                if (location != null) {
                    // Got location! Center map and show it
                    Log.d("MainActivity", "Location found: ${location.latitude}, ${location.longitude}")
                    showMapWithLocation(location)
                } else if (attempts < maxAttempts) {
                    // Try again in 500ms
                    Log.d("MainActivity", "Waiting for location... attempt $attempts")
                    searchEditText.postDelayed(this, 500)
                } else {
                    // Timeout - show map anyway with fallback message
                    Log.w("MainActivity", "Location timeout - showing map without centering")
                    showMapWithoutLocation()
                }
            }
        }
        
        // Start checking for location
        searchEditText.postDelayed(locationChecker, 500)
    }
    
    private fun showMapWithLocation(location: android.location.Location) {
        if (!mapReadyForDisplay) {
            val success = mapController.centerOnUserLocation()
            if (success) {
                Log.d("MainActivity", "Centered on user location at startup")
            }
            showMapAndHideLoading()
        }
    }
    
    private fun showMapWithoutLocation() {
        if (!mapReadyForDisplay) {
            Log.d("MainActivity", "Showing map without location centering")
            showMapAndHideLoading()
            Toast.makeText(this, "Could not determine location", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showMapAndHideLoading() {
        mapReadyForDisplay = true
        loadingOverlay.visibility = View.GONE
        Log.d("MainActivity", "Map is now visible")
    }

    private fun performAISearch(keyword: String) {
        if (::mapController.isInitialized) {
            mapController.clearPOIMarkers()
        }
        poiAdapter.clearResults()
        
        val userLocation = if (::mapController.isInitialized) mapController.getUserLocation() else null
        aiSearchManager.performAISearch(keyword, userLocation)
    }
    
    private fun performPOISearch(keyword: String) {
        if (::mapController.isInitialized) {
            mapController.clearPOIMarkers()
            mapController.clearCurrentRoute() // Clear any existing route
        }
        poiAdapter.clearResults()
        
        val userLocation = if (::mapController.isInitialized) mapController.getUserLocation() else null
        poiSearchManager.performKeywordSearch(keyword, userLocation)
    }

    private fun clearSearchResults() {
        if (::mapController.isInitialized) {
            mapController.clearPOIMarkers()
            mapController.clearCurrentRoute() // Clear any existing route
        }
        poiAdapter.clearResults()
        hideResultsWithAnimation()
    }

    private fun hideResultsWithAnimation() {
        resultsContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                resultsContainer.visibility = View.GONE
            }
    }

    private fun showResultsWithAnimation() {
        resultsContainer.visibility = View.VISIBLE
        resultsContainer.alpha = 0f
        resultsContainer.animate()
            .alpha(1f)
            .setDuration(300)
    }

    private fun handleAISearchResult(poiItems: List<PoiItem>?, success: Boolean, message: String, aiInfo: AIProcessedQuery?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        // Show AI processing info if available
        aiInfo?.let { info ->
            if (!info.isFallback) {
                val aiMessage = when {
                    info.searchKeywords.size > 1 -> "AI found ${info.searchKeywords.size} related keywords"
                    info.translatedQuery != info.originalQuery -> "AI translated: ${info.translatedQuery}"
                    else -> "AI processed your query"
                }
                searchUIHandler.showAIResultInfo(aiMessage)
                // Debug info omitted for brevity
            }
        }
        
        if (success && poiItems != null) {
            val userLocation = if (::mapController.isInitialized) mapController.getUserLocation() else null
            
            // Add markers to map
            mapController.addPOIMarkers(poiItems, userLocation)
            val displayItems = searchResultsProcessor.processResults(poiItems, userLocation)
            poiAdapter.updateResults(displayItems)
            showResultsWithAnimation()
            mapController.centerOnResults(poiItems, userLocation)
        } else {
            hideResultsWithAnimation()
        }
    }

    private fun handleSearchResult(poiItems: List<PoiItem>?, success: Boolean, message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        if (success && poiItems != null) {
            val userLocation = if (::mapController.isInitialized) mapController.getUserLocation() else null
            
            // Add markers to map
            mapController.addPOIMarkers(poiItems, userLocation)
            val displayItems = searchResultsProcessor.processResults(poiItems, userLocation)
            poiAdapter.updateResults(displayItems)
            showResultsWithAnimation()
            mapController.centerOnResults(poiItems, userLocation)
        } else {
            hideResultsWithAnimation()
        }
    }

    private fun focusOnPOI(poiItem: PoiItem) {
        mapController.focusOnPOI(poiItem)
        Toast.makeText(this, "Focused on: ${poiItem.title}", Toast.LENGTH_SHORT).show()
    }

    private fun handleRouteResult(success: Boolean, message: String, result: RouteController.RouteResult?) {
        Log.d("MainActivity", "Route result: success=$success, message=$message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        if (success && result != null) {
            // Route planning successful - show route overlay on map
            Log.d("MainActivity", "Route planning successful: ${result.routeData.distance}m, ${result.routeData.duration}s")
            mapController.showWalkingRoute(result)
        }
    }

    /**
     * Plan route from the Directions button in POI details
     */
    private fun planRouteFromDirectionsButton(poiDisplayItem: POIDisplayItem) {
        val userLocation = if (::mapController.isInitialized) mapController.getUserLocation() else null
        if (userLocation != null) {
            // Clear search results and POI markers - switch to route mode
            clearSearchResults()
            
            // Update search bar to show route destination
            searchEditText.setText("Directions to ${poiDisplayItem.title}")
            clearButton.visibility = View.VISIBLE
            
            val startPoint = LatLonPoint(userLocation.latitude, userLocation.longitude)
            val endPoint = LatLonPoint(
                poiDisplayItem.poiItem.latLonPoint.latitude, 
                poiDisplayItem.poiItem.latLonPoint.longitude
            )
            
            Log.d("MainActivity", "Planning route to ${poiDisplayItem.title}")
            Toast.makeText(this, "Planning route to ${poiDisplayItem.title}...", Toast.LENGTH_SHORT).show()
            routeController.planWalkingRoute(startPoint, endPoint)
        } else {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
        }
    }

    // --- The MapView lifecycle methods MUST stay in the Activity ---
    override fun onResume() {
        super.onResume()
        if (::aMap.isInitialized) {
            mapView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::aMap.isInitialized) {
            mapView.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::poiSearchManager.isInitialized) {
            poiSearchManager.cleanup()
        }
        if (::aiSearchManager.isInitialized) {
            aiSearchManager.cleanup()
        }
        if (::routeController.isInitialized) {
            routeController.cleanup()
        }
        if (::mapController.isInitialized) {
            mapController.cleanup()
        }
        if (::aMap.isInitialized) {
            mapView.onDestroy()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::aMap.isInitialized) {
            mapView.onSaveInstanceState(outState)
        }
    }
}