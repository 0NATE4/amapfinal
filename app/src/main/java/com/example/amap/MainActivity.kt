package com.example.amap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.example.amap.map.MapViewModel // Import the ViewModel from its new package
import com.example.amap.util.AmapPrivacy // Import the helper from its new package
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var aMap: AMap

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

        // 3. Start the process of checking permissions and observing the ViewModel's state.
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