package com.example.amap

import com.amap.api.services.core.PoiItem
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class POIResultsAdapterTest {

    @Test
    fun `POIDisplayItem should store data correctly`() {
        // Given - test data
        val mockPoiItem = mockk<PoiItem>()
        val title = "Test Restaurant"
        val address = "123 Test Street"
        
        // When - creating POIDisplayItem
        val displayItem = POIDisplayItem(title, address, mockPoiItem)
        
        // Then - data should be stored correctly
        assertEquals(title, displayItem.title)
        assertEquals(address, displayItem.address)
        assertEquals(mockPoiItem, displayItem.poiItem)
    }

    @Test
    fun `POIDisplayItem creation with empty data should work`() {
        // Given - empty data
        val mockPoiItem = mockk<PoiItem>()
        val title = ""
        val address = ""
        
        // When - creating POIDisplayItem
        val displayItem = POIDisplayItem(title, address, mockPoiItem)
        
        // Then - should handle empty strings
        assertEquals("", displayItem.title)
        assertEquals("", displayItem.address)
        assertEquals(mockPoiItem, displayItem.poiItem)
    }

    @Test
    fun `POIDisplayItem equality should work correctly`() {
        // Given - two identical POIDisplayItems
        val mockPoiItem = mockk<PoiItem>()
        val item1 = POIDisplayItem("Restaurant", "123 Main St", mockPoiItem)
        val item2 = POIDisplayItem("Restaurant", "123 Main St", mockPoiItem)
        
        // When/Then - they should be equal (data class equality)
        assertEquals(item1, item2)
        assertEquals(item1.hashCode(), item2.hashCode())
    }

    @Test
    fun `POIDisplayItem with different data should not be equal`() {
        // Given - two different POIDisplayItems
        val mockPoiItem1 = mockk<PoiItem>()
        val mockPoiItem2 = mockk<PoiItem>()
        val item1 = POIDisplayItem("Restaurant A", "123 Main St", mockPoiItem1)
        val item2 = POIDisplayItem("Restaurant B", "456 Oak Ave", mockPoiItem2)
        
        // When/Then - they should not be equal
        assertNotEquals(item1, item2)
    }

    // Helper function to create test data
    private fun createTestPOIItems(count: Int): List<POIDisplayItem> {
        return (1..count).map { i ->
            POIDisplayItem(
                title = "POI $i",
                address = "Address $i",
                poiItem = mockk<PoiItem>(relaxed = true)
            )
        }
    }

    @Test
    fun `createTestPOIItems helper should generate correct count`() {
        // Given - request for 5 items
        val items = createTestPOIItems(5)
        
        // When/Then - should get exactly 5 items
        assertEquals(5, items.size)
        assertEquals("POI 1", items[0].title)
        assertEquals("POI 5", items[4].title)
        assertEquals("Address 1", items[0].address)
        assertEquals("Address 5", items[4].address)
    }
} 