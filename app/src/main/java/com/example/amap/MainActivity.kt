package com.example.amap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.amap.api.services.core.LatLonPoint
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
    private lateinit var nearbyButton: Button
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var poiAdapter: POIResultsAdapter
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
        nearbyButton = findViewById(R.id.nearbyButton)
        setupSearch()
        
        // 4. Setup results list
        setupResultsList()

        // 5. Start the process of checking permissions and observing the ViewModel's state.
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
        
        nearbyButton.setOnClickListener {
            performNearbySearch()
        }
    }

    private fun setupResultsList() {
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        poiAdapter = POIResultsAdapter { poiDisplayItem ->
            // When user clicks on list item, focus on corresponding marker
            focusOnPOI(poiDisplayItem.poiItem)
        }
        
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = poiAdapter
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
        
        // Check if we can do nearby search based on user location
        val myLocation = aMap.myLocation
        if (myLocation != null) {
            // Use nearby search around user's location (following Amap docs)
            val userLatLonPoint = LatLonPoint(myLocation.latitude, myLocation.longitude)
            val searchBound = PoiSearch.SearchBound(userLatLonPoint, 1000) // 1000 meters radius
            poiSearch?.bound = searchBound
            
            Log.d("POISearch", "Using nearby search around user location: ${myLocation.latitude}, ${myLocation.longitude}")
            Toast.makeText(this, "Searching for $keyword nearby...", Toast.LENGTH_SHORT).show()
        } else {
            // Fall back to general keyword search
            Log.d("POISearch", "User location not available, using general search")
            Toast.makeText(this, "Searching for $keyword...", Toast.LENGTH_SHORT).show()
        }
        
        // Step 4: Call searchPOIAsyn() method to send request
        poiSearch?.searchPOIAsyn()
    }

    private fun performNearbySearch() {
        Log.d("POISearch", "Starting nearby search")
        
        val myLocation = aMap.myLocation
        if (myLocation == null) {
            Toast.makeText(this, "Location not available. Please enable location services.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Clear previous markers
        clearPOIMarkers()
        
        // Search for general POIs nearby (empty keyword means all types)
        val query = PoiSearch.Query("", "", "")  // Empty keyword for general nearby search
        query.pageSize = 20  // More results for nearby search
        query.pageNum = 1
        
        poiSearch = PoiSearch(this, query)
        poiSearch?.setOnPoiSearchListener(this)
        
        // Set search bound around user location
        val userLatLonPoint = LatLonPoint(myLocation.latitude, myLocation.longitude)
        val searchBound = PoiSearch.SearchBound(userLatLonPoint, 500) // 500 meters for nearby discovery
        poiSearch?.bound = searchBound
        
        Log.d("POISearch", "Searching nearby POIs around: ${myLocation.latitude}, ${myLocation.longitude}")
        Toast.makeText(this, "Finding nearby places...", Toast.LENGTH_SHORT).show()
        
        poiSearch?.searchPOIAsyn()
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
        val myLocation = aMap.myLocation
        val displayItems = mutableListOf<POIDisplayItem>()
        
        for (poi in poiItems) {
            val latLng = LatLng(poi.latLonPoint.latitude, poi.latLonPoint.longitude)
            
            // Calculate distance if we have user location
            val snippet = if (myLocation != null) {
                val distance = calculateDistance(myLocation.latitude, myLocation.longitude, 
                                               poi.latLonPoint.latitude, poi.latLonPoint.longitude)
                "${poi.snippet ?: poi.adName} • ${distance}m away"
            } else {
                poi.snippet ?: poi.adName
            }
            
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(poi.title ?: "Unknown POI")
                    .snippet(snippet)
            )
            
            poiMarkers.add(marker)
            
            // Create display item for list
            val displayItem = POIDisplayItem(
                title = poi.title ?: "Unknown POI",
                address = snippet,
                poiItem = poi
            )
            displayItems.add(displayItem)
            
            Log.d("POISearch", "Added marker: ${poi.title} at ${poi.latLonPoint.latitude}, ${poi.latLonPoint.longitude}")
        }
        
        // Update the list
        poiAdapter.updateResults(displayItems)
        resultsRecyclerView.visibility = if (displayItems.isNotEmpty()) RecyclerView.VISIBLE else RecyclerView.GONE
        
        // Move camera to show user location and first result
        if (poiItems.isNotEmpty()) {
            if (myLocation != null) {
                // If we have user location, center between user and POIs
                aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                    LatLng(myLocation.latitude, myLocation.longitude), 14f))
            } else {
                // Otherwise center on first POI
                val firstPoi = poiItems[0]
                val firstLatLng = LatLng(firstPoi.latLonPoint.latitude, firstPoi.latLonPoint.longitude)
                aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(firstLatLng, 15f))
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c).toInt()
    }

    private fun clearPOIMarkers() {
        for (marker in poiMarkers) {
            marker.remove()
        }
        poiMarkers.clear()
        
        // Also clear the list
        poiAdapter.clearResults()
        resultsRecyclerView.visibility = RecyclerView.GONE
    }

    private fun focusOnPOI(poiItem: PoiItem) {
        val latLng = LatLng(poiItem.latLonPoint.latitude, poiItem.latLonPoint.longitude)
        aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        
        // Find and show info window for this POI
        for (marker in poiMarkers) {
            if (marker.position.latitude == latLng.latitude && marker.position.longitude == latLng.longitude) {
                marker.showInfoWindow()
                break
            }
        }
        
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
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}