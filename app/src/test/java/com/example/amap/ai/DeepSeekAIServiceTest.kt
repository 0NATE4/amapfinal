package com.example.amap.ai

import android.location.Location
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class DeepSeekAIServiceTest {
    
    @Test
    fun testAIServiceInitialization() {
        val aiService = DeepSeekAIService()
        assertNotNull(aiService)
    }
    
    @Test
    fun testAIProcessedQueryDataClass() {
        val query = AIProcessedQuery(
            originalQuery = "dumplings",
            translatedQuery = "饺子",
            searchKeywords = listOf("饺子", "饺子店", "中餐"),
            confidence = 0.95,
            explanation = "Translated English to Chinese",
            isFallback = false
        )
        
        assertEquals("dumplings", query.originalQuery)
        assertEquals("饺子", query.translatedQuery)
        assertEquals(3, query.searchKeywords.size)
        assertEquals(0.95, query.confidence, 0.01)
        assertFalse(query.isFallback)
    }
    
    @Test
    fun testFallbackQuery() {
        val fallbackQuery = AIProcessedQuery(
            originalQuery = "test",
            translatedQuery = "test",
            searchKeywords = listOf("test"),
            confidence = 0.0,
            isFallback = true
        )
        
        assertTrue(fallbackQuery.isFallback)
        assertEquals(0.0, fallbackQuery.confidence, 0.01)
    }
    
    @Test
    fun testPromptBuilding() {
        val aiService = DeepSeekAIService()
        val location = Location("test").apply {
            latitude = 39.9042
            longitude = 116.4074
        }
        
        // Use reflection to access private method for testing
        val promptMethod = aiService.javaClass.getDeclaredMethod("buildPrompt", String::class.java, Location::class.java)
        promptMethod.isAccessible = true
        val prompt = promptMethod.invoke(aiService, "kfc", location) as String
        
        assertTrue("Prompt should contain user query", prompt.contains("kfc"))
        assertTrue("Prompt should contain location", prompt.contains("39.9042"))
        assertTrue("Prompt should contain JSON format instructions", prompt.contains("JSON format"))
    }
} 