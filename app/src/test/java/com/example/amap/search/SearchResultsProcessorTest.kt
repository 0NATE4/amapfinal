package com.example.amap.search

import android.location.Location
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.example.amap.data.model.POIDisplayItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SearchResultsProcessorTest {

    private lateinit var processor: SearchResultsProcessor

    @Before
    fun setup() {
        processor = SearchResultsProcessor()
    }

    @Test
    fun `processResults with no user location should create display items without distance`() {
        // Given - POI items without user location
        val mockPoi = createMockPoi("Test Restaurant", "123 Main St", 40.7128, -74.0060)
        val poiItems = listOf(mockPoi)
        
        // When - processing results
        val displayItems = processor.processResults(poiItems, null)
        
        // Then - should create display items without distance
        assertEquals(1, displayItems.size)
        assertEquals("Test Restaurant", displayItems[0].title)
        assertEquals("123 Main St", displayItems[0].address)
        assertEquals(mockPoi, displayItems[0].poiItem)
    }

    @Test
    fun `processResults with user location should include distance in address`() {
        // Given - POI items with user location
        val mockPoi = createMockPoi("Coffee Shop", "456 Oak Ave", 40.7589, -73.9851)
        val poiItems = listOf(mockPoi)
        
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns 40.7128
        every { mockLocation.longitude } returns -74.0060
        
        // When - processing results
        val displayItems = processor.processResults(poiItems, mockLocation)
        
        // Then - should include distance in address
        assertEquals(1, displayItems.size)
        assertEquals("Coffee Shop", displayItems[0].title)
        assertTrue("Address should contain distance", displayItems[0].address.contains("m away"))
        assertTrue("Address should contain original snippet", displayItems[0].address.contains("456 Oak Ave"))
    }

    @Test
    fun `processResults with multiple POIs should process all items`() {
        // Given - multiple POI items
        val poi1 = createMockPoi("Restaurant 1", "Address 1", 40.7128, -74.0060)
        val poi2 = createMockPoi("Restaurant 2", "Address 2", 40.7589, -73.9851)
        val poi3 = createMockPoi("Restaurant 3", "Address 3", 40.7505, -73.9934)
        val poiItems = listOf(poi1, poi2, poi3)
        
        // When - processing results
        val displayItems = processor.processResults(poiItems, null)
        
        // Then - should process all items
        assertEquals(3, displayItems.size)
        assertEquals("Restaurant 1", displayItems[0].title)
        assertEquals("Restaurant 2", displayItems[1].title)
        assertEquals("Restaurant 3", displayItems[2].title)
    }

    @Test
    fun `processResults with empty list should return empty list`() {
        // Given - empty POI list
        val poiItems = emptyList<PoiItem>()
        
        // When - processing results
        val displayItems = processor.processResults(poiItems, null)
        
        // Then - should return empty list
        assertTrue(displayItems.isEmpty())
    }

    @Test
    fun `processResults should handle POI with null title and snippet`() {
        // Given - POI with null values
        val mockPoi = mockk<PoiItem>()
        every { mockPoi.title } returns null
        every { mockPoi.snippet } returns null
        every { mockPoi.adName } returns "Fallback Address"
        every { mockPoi.latLonPoint } returns LatLonPoint(40.7128, -74.0060)
        
        val poiItems = listOf(mockPoi)
        
        // When - processing results
        val displayItems = processor.processResults(poiItems, null)
        
        // Then - should handle null values gracefully
        assertEquals(1, displayItems.size)
        assertEquals("Unknown POI", displayItems[0].title)
        assertEquals("Fallback Address", displayItems[0].address)
    }

    private fun createMockPoi(title: String, address: String, lat: Double, lng: Double): PoiItem {
        val mockPoi = mockk<PoiItem>()
        every { mockPoi.title } returns title
        every { mockPoi.snippet } returns address
        every { mockPoi.adName } returns address
        every { mockPoi.latLonPoint } returns LatLonPoint(lat, lng)
        return mockPoi
    }
} 