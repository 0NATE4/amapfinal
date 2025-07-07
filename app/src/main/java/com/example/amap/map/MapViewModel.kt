package com.example.amap.map

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.model.MyLocationStyle
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
        // This is pure map configuration logic, it belongs here.
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
        myLocationStyle.interval(2000)

        aMap.myLocationStyle = myLocationStyle
        aMap.isMyLocationEnabled = true
    }
}

// Data class to hold all the UI state for our map screen
data class MapUiState(
    val isLocationPermissionGranted: Boolean = false
)