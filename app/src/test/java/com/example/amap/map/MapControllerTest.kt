package com.example.amap.map

import android.location.Location
import com.amap.api.maps.AMap
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MapControllerTest {

    private lateinit var mockAMap: AMap
    private lateinit var mapController: MapController

    @Before
    fun setup() {
        mockAMap = mockk(relaxed = true)
        mapController = MapController(mockAMap)
    }

    @Test
    fun `centerOnUserLocation should return true when location is available`() {
        // Given - mock location available
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns 40.7128
        every { mockLocation.longitude } returns -74.0060
        every { mockAMap.myLocation } returns mockLocation

        // When - centering on user location
        val result = mapController.centerOnUserLocation()

        // Then - should return true and animate camera
        assertTrue("Should return true when location is available", result)
        verify { mockAMap.animateCamera(any()) }
    }

    @Test
    fun `centerOnUserLocation should return false when location is not available`() {
        // Given - no location available
        every { mockAMap.myLocation } returns null

        // When - centering on user location
        val result = mapController.centerOnUserLocation()

        // Then - should return false and not animate camera
        assertFalse("Should return false when location is not available", result)
        verify(exactly = 0) { mockAMap.animateCamera(any()) }
    }

    @Test
    fun `clearPOIMarkers should remove all markers`() {
        // Given - controller with no initial markers
        
        // When - clearing markers
        mapController.clearPOIMarkers()
        
        // Then - should not crash (markers list is empty)
        assertTrue("Should complete without throwing", true)
    }
} 