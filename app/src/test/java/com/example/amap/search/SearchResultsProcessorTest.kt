package com.example.amap.search

import android.location.Location
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.example.amap.data.model.POIDisplayItem
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class SearchResultsProcessorTest {
    
    private val processor = SearchResultsProcessor()
    
    @Test
    fun `processResults with no user location should create display items without distance`() {
        val poiItems = listOf(
            createMockPoiItem("米村拌饭", "大连市中山区", 38.9, 121.5)
        )
        
        val displayItems = processor.processResults(poiItems, null)
        
        assertEquals(1, displayItems.size)
        assertEquals("米村拌饭", displayItems[0].title)
        assertEquals("大连市中山区", displayItems[0].address)
        assertEquals("N/A", displayItems[0].distance)
    }
    
    @Test
    fun `processResults with user location should include distance in address`() {
        val mockLocation = Location("test").apply {
            latitude = 38.9
            longitude = 121.5
        }
        
        val poiItems = listOf(
            createMockPoiItem("星巴克", "大连市西岗区", 38.91, 121.51)
        )
        
        val displayItems = processor.processResults(poiItems, mockLocation)
        
        assertEquals(1, displayItems.size)
        assertTrue(displayItems[0].distance.isNotEmpty())
        assertNotEquals("N/A", displayItems[0].distance)
    }
    
    @Test
    fun `processResults with multiple POIs should process all items`() {
        val poiItems = listOf(
            createMockPoiItem("米村拌饭", "大连市中山区", 38.9, 121.5),
            createMockPoiItem("星巴克", "大连市西岗区", 38.91, 121.51)
        )
        
        val displayItems = processor.processResults(poiItems, null)
        
        assertEquals(2, displayItems.size)
        assertEquals("米村拌饭", displayItems[0].title)
        assertEquals("星巴克", displayItems[1].title)
    }
    
    @Test
    fun `processResults with empty list should return empty list`() {
        val poiItems = emptyList<PoiItem>()
        
        val displayItems = processor.processResults(poiItems, null)
        
        assertTrue(displayItems.isEmpty())
    }
    
    @Test
    fun `processResults should handle POI with null title and snippet`() {
        val poiItems = listOf(
            createMockPoiItem(null, null, 38.9, 121.5)
        )
        
        val displayItems = processor.processResults(poiItems, null)
        
        assertEquals(1, displayItems.size)
        assertEquals("Unknown POI", displayItems[0].title)
        assertEquals("Unknown address", displayItems[0].address)
    }
    
    @Test
    fun `bilingual display should show both Chinese and English when available`() {
        val poiItems = listOf(
            createMockPoiItem("米村拌饭", "大连市中山区", 38.9, 121.5)
        )
        
        runBlocking {
            val displayItems = processor.processResultsWithTranslations(poiItems, null)
            
            assertEquals(1, displayItems.size)
            assertEquals("米村拌饭", displayItems[0].title)
            assertEquals("大连市中山区", displayItems[0].address)
            
            // English translations should be available (may be null if translation fails)
            assertNotNull(displayItems[0].englishTitle)
            assertNotNull(displayItems[0].englishAddress)
        }
    }
    
    private fun createMockPoiItem(title: String?, snippet: String?, lat: Double, lng: Double): PoiItem {
        return PoiItem("test_id", LatLonPoint(lat, lng), title, snippet)
    }
} 