package com.example.amap.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        viewModel = MapViewModel()
    }

    @Test
    fun `initial state should have permission not granted`() = runTest {
        // Given - fresh ViewModel
        // When - checking initial state
        val initialState = viewModel.uiState.first()
        
        // Then - permission should be false by default
        assertFalse(initialState.isLocationPermissionGranted)
    }

    @Test
    fun `onPermissionResult with true should update state to granted`() = runTest {
        // Given - fresh ViewModel
        
        // When - permission is granted
        viewModel.onPermissionResult(true)
        
        // Then - state should reflect permission granted
        val updatedState = viewModel.uiState.first()
        assertTrue(updatedState.isLocationPermissionGranted)
    }

    @Test
    fun `onPermissionResult with false should keep state as not granted`() = runTest {
        // Given - ViewModel with granted permission
        viewModel.onPermissionResult(true)
        
        // When - permission is revoked
        viewModel.onPermissionResult(false)
        
        // Then - state should reflect permission not granted
        val updatedState = viewModel.uiState.first()
        assertFalse(updatedState.isLocationPermissionGranted)
    }

    @Test
    fun `state flow should be cold and reusable`() = runTest {
        // Given - fresh ViewModel
        
        // When - collecting state multiple times
        val state1 = viewModel.uiState.first()
        val state2 = viewModel.uiState.first()
        
        // Then - should get same initial state
        assertEquals(state1.isLocationPermissionGranted, state2.isLocationPermissionGranted)
        assertFalse(state1.isLocationPermissionGranted)
        assertFalse(state2.isLocationPermissionGranted)
    }

    @Test
    fun `multiple permission changes should update state correctly`() = runTest {
        // Given - fresh ViewModel
        
        // When - multiple permission changes
        viewModel.onPermissionResult(true)
        var state = viewModel.uiState.first()
        assertTrue(state.isLocationPermissionGranted)
        
        viewModel.onPermissionResult(false)
        state = viewModel.uiState.first()
        assertFalse(state.isLocationPermissionGranted)
        
        viewModel.onPermissionResult(true)
        state = viewModel.uiState.first()
        assertTrue(state.isLocationPermissionGranted)
    }
} 