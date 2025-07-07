package com.example.amap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.example.amap.map.MapViewModel // Import the ViewModel from its new package
import com.example.amap.util.AmapPrivacy // Import the helper from its new package
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), PoiSearch.OnPoiSearchListener {

    private lateinit var mapView: MapView
    private lateinit var aMap: AMap
    private lateinit var searchEditText: EditText
    private var poiSearch: PoiSearch? = null
    private val poiMarkers = mutableListOf<Marker>()

    // Get a reference to our ViewModel using the 'by viewModels()' delegate.
    // This correctly handles the ViewModel's lifecycle.
    private val viewModel: MapViewModel by viewModels()

    // The permission launcher now just sends the result to the ViewModel.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.d("Permission", "Permission result received: $isGranted")
            viewModel.onPermissionResult(isGranted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Use our isolated helper to handle privacy compliance. Clean!
        AmapPrivacy.ensureCompliance(this)

        setContentView(R.layout.activity_main)

        // 2. Standard MapView setup
        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        aMap = mapView.map

        // 3. Setup search functionality
        searchEditText = findViewById(R.id.searchEditText)
        setupSearch()

        // 4. Start the process of checking permissions and observing the ViewModel's state.
        checkInitialPermissions()
        observeUiState()
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
        // This is the core of the View's logic. It listens for any state changes
        // from the ViewModel and updates the UI accordingly.
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isLocationPermissionGranted) {
                    Log.d("MainActivity", "State updated: Permission is now granted.")
                    viewModel.setupLocationStyle(aMap) // Tell the ViewModel to configure the map
                } else {
                    Log.d("MainActivity", "State updated: Permission is not granted.")
                }
            }
        }
    }

    private fun setupSearch() {
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performPOISearch(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun performPOISearch(keyword: String) {
        Log.d("POISearch", "Starting search for: $keyword")
        
        // Clear previous markers
        clearPOIMarkers()
        
        // Step 1 & 2: Construct PoiSearch.Query object following Amap docs
        val query = PoiSearch.Query(keyword, "", "")  // keyword, category, city
        query.pageSize = 10  // Set max POI items per page
        query.pageNum = 1    // Set query page number
        
        // Step 3: Construct PoiSearch object and set up monitoring
        poiSearch = PoiSearch(this, query)
        poiSearch?.setOnPoiSearchListener(this)
        
        // Step 4: Call searchPOIAsyn() method to send request
        poiSearch?.searchPOIAsyn()
        
        Toast.makeText(this, "Searching for $keyword...", Toast.LENGTH_SHORT).show()
    }

    // Step 5: Parse returned results through callback interface
    override fun onPoiSearched(result: PoiResult?, rCode: Int) {
        Log.d("POISearch", "Search result received. Code: $rCode")
        
        if (rCode == 1000 && result != null) {  // 1000 means success
            val poiItems = result.pois
            if (poiItems != null && poiItems.isNotEmpty()) {
                Log.d("POISearch", "Found ${poiItems.size} POIs")
                displayPOIResults(poiItems)
                Toast.makeText(this, "Found ${poiItems.size} results", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("POISearch", "No POI results found")
                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("POISearch", "Search failed with code: $rCode")
            Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPoiItemSearched(item: PoiItem?, rCode: Int) {
        // Not used for basic keyword search
    }

    private fun displayPOIResults(poiItems: List<PoiItem>) {
        for (poi in poiItems) {
            val latLng = LatLng(poi.latLonPoint.latitude, poi.latLonPoint.longitude)
            
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(poi.title ?: "Unknown POI")
                    .snippet(poi.snippet ?: poi.adName)
            )
            
            poiMarkers.add(marker)
            
            Log.d("POISearch", "Added marker: ${poi.title} at ${poi.latLonPoint.latitude}, ${poi.latLonPoint.longitude}")
        }
        
        // Move camera to first result
        if (poiItems.isNotEmpty()) {
            val firstPoi = poiItems[0]
            val firstLatLng = LatLng(firstPoi.latLonPoint.latitude, firstPoi.latLonPoint.longitude)
            aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(firstLatLng, 15f))
        }
    }

    private fun clearPOIMarkers() {
        for (marker in poiMarkers) {
            marker.remove()
        }
        poiMarkers.clear()
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
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}