package com.example.amap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.services.core.PoiItem
import com.example.amap.data.model.POIDisplayItem
import com.example.amap.map.MapViewModel
import com.example.amap.map.MapController
import com.example.amap.search.POISearchManager
import com.example.amap.search.SearchResultsProcessor
import com.example.amap.ui.POIResultsAdapter
import com.example.amap.ui.SearchUIHandler
import com.example.amap.util.AmapPrivacy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var aMap: AMap
    private lateinit var searchEditText: EditText
    private lateinit var myLocationButton: FloatingActionButton
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var poiAdapter: POIResultsAdapter
    
    // Components
    private lateinit var mapController: MapController
    private lateinit var poiSearchManager: POISearchManager
    private lateinit var searchResultsProcessor: SearchResultsProcessor
    private lateinit var searchUIHandler: SearchUIHandler

    private val viewModel: MapViewModel by viewModels()

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
        initializeComponents()
        setupUI()
        
        checkInitialPermissions()
        observeUiState()
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        aMap = mapView.map
        
        searchEditText = findViewById(R.id.searchEditText)
        myLocationButton = findViewById(R.id.myLocationButton)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
    }

    private fun initializeComponents() {
        mapController = MapController(aMap)
        poiSearchManager = POISearchManager(this) { poiItems, success, message ->
            handleSearchResult(poiItems, success, message)
        }
        searchResultsProcessor = SearchResultsProcessor()
        searchUIHandler = SearchUIHandler(
            searchEditText = searchEditText,
            onSearch = { query -> performPOISearch(query) }
        )
        
        poiAdapter = POIResultsAdapter { poiDisplayItem ->
            focusOnPOI(poiDisplayItem.poiItem)
        }
    }

    private fun setupUI() {
        searchUIHandler.setupSearchListeners()
        
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = poiAdapter
        
        // Setup my location button
        myLocationButton.setOnClickListener {
            val success = mapController.centerOnUserLocation()
            if (!success) {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
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
                    viewModel.setupLocationStyle(aMap)
                    
                    // Center on user location when first opening the app
                    centerOnUserLocationInitial()
                } else {
                    Log.d("MainActivity", "State updated: Permission is not granted.")
                }
            }
        }
    }

    private fun centerOnUserLocationInitial() {
        // Give the map a moment to get location after permission is granted
        searchEditText.postDelayed({
            val success = mapController.centerOnUserLocation()
            if (success) {
                Log.d("MainActivity", "Centered on user location at startup")
            } else {
                Log.d("MainActivity", "Could not center on user location at startup")
            }
        }, 1000) // Wait 1 second for location to be available
    }

    private fun performPOISearch(keyword: String) {
        mapController.clearPOIMarkers()
        poiAdapter.clearResults()
        
        val userLocation = aMap.myLocation
        poiSearchManager.performKeywordSearch(keyword, userLocation)
    }



    private fun handleSearchResult(poiItems: List<PoiItem>?, success: Boolean, message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        if (success && poiItems != null) {
            val userLocation = aMap.myLocation
            
            // Add markers to map
            mapController.addPOIMarkers(poiItems, userLocation)
            
            // Process results for list display
            val displayItems = searchResultsProcessor.processResults(poiItems, userLocation)
            poiAdapter.updateResults(displayItems)
            
            // Show/hide results list
            resultsRecyclerView.visibility = if (displayItems.isNotEmpty()) {
                RecyclerView.VISIBLE
            } else {
                RecyclerView.GONE
            }
            
            // Center camera on results
            mapController.centerOnResults(poiItems, userLocation)
        } else {
            resultsRecyclerView.visibility = RecyclerView.GONE
        }
    }

    private fun focusOnPOI(poiItem: PoiItem) {
        mapController.focusOnPOI(poiItem)
        Toast.makeText(this, "Focused on: ${poiItem.title}", Toast.LENGTH_SHORT).show()
    }

    // --- The MapView lifecycle methods MUST stay in the Activity ---
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        poiSearchManager.cleanup()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}