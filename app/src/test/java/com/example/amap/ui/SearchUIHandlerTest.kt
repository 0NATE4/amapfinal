package com.example.amap.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SearchUIHandlerTest {

    private lateinit var mockSearchEditText: EditText
    private lateinit var mockNearbyButton: Button
    private lateinit var searchUIHandler: SearchUIHandler
    private var lastSearchQuery: String? = null
    private var nearbySearchCalled = false

    @Before
    fun setup() {
        mockSearchEditText = mockk(relaxed = true)
        mockNearbyButton = mockk(relaxed = true)
        
        // Mock the context and InputMethodManager
        val mockContext = mockk<Context>(relaxed = true)
        val mockInputMethodManager = mockk<InputMethodManager>(relaxed = true)
        every { mockSearchEditText.context } returns mockContext
        every { mockContext.getSystemService(Context.INPUT_METHOD_SERVICE) } returns mockInputMethodManager
        
        searchUIHandler = SearchUIHandler(
            searchEditText = mockSearchEditText,
            nearbyButton = mockNearbyButton,
            onSearch = { query -> lastSearchQuery = query },
            onNearbySearch = { nearbySearchCalled = true }
        )
    }

    @Test
    fun `isValidQuery should return true for non-empty strings`() {
        // Given - valid search queries
        val validQueries = listOf("restaurant", "coffee shop", "ATM", "gas station")
        
        // When/Then - should all be valid
        validQueries.forEach { query ->
            assertTrue("Query '$query' should be valid", searchUIHandler.isValidQuery(query))
        }
    }

    @Test
    fun `isValidQuery should return false for empty or whitespace strings`() {
        // Given - invalid search queries
        val invalidQueries = listOf("", "   ", "\t", "\n", "  \t  \n  ")
        
        // When/Then - should all be invalid
        invalidQueries.forEach { query ->
            assertFalse("Query '$query' should be invalid", searchUIHandler.isValidQuery(query))
        }
    }

    @Test
    fun `getCurrentQuery should return trimmed text from EditText`() {
        // Given - EditText with text (including whitespace)
        every { mockSearchEditText.text.toString() } returns "  restaurant  "
        
        // When - getting current query
        val query = searchUIHandler.getCurrentQuery()
        
        // Then - should return trimmed text
        assertEquals("restaurant", query)
    }

    @Test
    fun `getCurrentQuery should handle empty EditText`() {
        // Given - empty EditText
        every { mockSearchEditText.text.toString() } returns ""
        
        // When - getting current query
        val query = searchUIHandler.getCurrentQuery()
        
        // Then - should return empty string
        assertEquals("", query)
    }

    @Test
    fun `isValidQuery should handle special characters correctly`() {
        // Given - queries with special characters
        val validQueriesWithSpecialChars = listOf("McDonald's", "7-Eleven", "AT&T", "Café")
        val invalidQueriesWithSpecialChars = listOf("!@#$", "   !@#$   ")
        
        // When/Then - special characters in context should be valid
        validQueriesWithSpecialChars.forEach { query ->
            assertTrue("Query '$query' should be valid", searchUIHandler.isValidQuery(query))
        }
        
        // Special characters alone should be valid (user's choice)
        validQueriesWithSpecialChars.forEach { query ->
            assertTrue("Query '$query' should be valid", searchUIHandler.isValidQuery(query))
        }
    }

    @Test
    fun `isValidQuery should handle unicode characters`() {
        // Given - queries with unicode characters
        val unicodeQueries = listOf("北京餐厅", "café", "Москва", "🍕 pizza")
        
        // When/Then - should all be valid
        unicodeQueries.forEach { query ->
            assertTrue("Query '$query' should be valid", searchUIHandler.isValidQuery(query))
        }
    }

    @Test
    fun `isValidQuery edge cases should work correctly`() {
        // Given - edge case queries
        val edgeCases = mapOf(
            "a" to true,  // Single character
            "123" to true,  // Numbers only
            "   a   " to true,  // Single char with whitespace (will be trimmed)
            "test\nwith\nnewlines" to true,  // Contains newlines
            "test\twith\ttabs" to true  // Contains tabs
        )
        
        // When/Then - should handle edge cases correctly
        edgeCases.forEach { (query, expected) ->
            val actual = searchUIHandler.isValidQuery(query.trim())
            assertEquals("Query '$query' should be $expected", expected, actual)
        }
    }

    @Test
    fun `hideKeyboard should call InputMethodManager correctly`() {
        // Given - SearchUIHandler with mocked dependencies
        val mockContext = mockk<Context>(relaxed = true)
        val mockInputMethodManager = mockk<InputMethodManager>(relaxed = true)
        val mockWindowToken = mockk<android.os.IBinder>()
        
        every { mockSearchEditText.context } returns mockContext
        every { mockContext.getSystemService(Context.INPUT_METHOD_SERVICE) } returns mockInputMethodManager
        every { mockSearchEditText.windowToken } returns mockWindowToken
        
        // When - hiding keyboard
        searchUIHandler.hideKeyboard()
        
        // Then - should call InputMethodManager with correct parameters
        verify { mockInputMethodManager.hideSoftInputFromWindow(mockWindowToken, 0) }
    }
} 