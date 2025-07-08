package com.example.amap.map

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.model.MyLocationStyle
import com.example.amap.core.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel : ViewModel() {

    // Private mutable state that can only be changed within the ViewModel
    private val _uiState = MutableStateFlow(MapUiState())
    // Public immutable state for the UI to observe
    val uiState = _uiState.asStateFlow()

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(isLocationPermissionGranted = isGranted)
    }

    fun setupLocationStyle(aMap: AMap) {
        // Configure location style to show user location without auto-following
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)  // Show location, don't follow
        myLocationStyle.interval(Constants.Map.LOCATION_UPDATE_INTERVAL)
        myLocationStyle.strokeColor(android.graphics.Color.argb(0, 0, 0, 0))  // Remove stroke
        myLocationStyle.radiusFillColor(android.graphics.Color.argb(0, 0, 0, 0))  // Remove accuracy circle

        aMap.myLocationStyle = myLocationStyle
        aMap.isMyLocationEnabled = true
    }
}

// Data class to hold all the UI state for our map screen
data class MapUiState(
    val isLocationPermissionGranted: Boolean = false
)