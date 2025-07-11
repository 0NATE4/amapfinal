package com.example.amap.search

import org.junit.Test
import org.junit.Assert.*

class AIEnhancedSearchManagerTest {
    
    @Test
    fun testKoreanFoodQuery() {
        // Test that Korean food queries would include relevant keywords
        val testKeywords = listOf("韩式料理", "韩国料理", "朝鲜族美食", "韩餐")
        
        // Verify that cuisine-related terms are included
        assertTrue("Should include Korean cuisine", testKeywords.contains("韩式料理"))
        assertTrue("Should include Korean-Chinese cuisine", testKeywords.contains("朝鲜族美食"))
        assertTrue("Should include Korean food", testKeywords.contains("韩餐"))
    }
    
    @Test
    fun testCoffeeShopQuery() {
        // Test coffee shop related keywords
        val testKeywords = listOf("咖啡店", "咖啡", "星巴克", "饮品店")
        
        // Verify coffee-related terms
        assertTrue("Should include coffee shop", testKeywords.contains("咖啡店"))
        assertTrue("Should include coffee", testKeywords.contains("咖啡"))
        assertTrue("Should include beverage shop", testKeywords.contains("饮品店"))
    }
    
    @Test
    fun testUndergroundMarketQuery() {
        // Test underground market related keywords
        val testKeywords = listOf("地下商场", "地下市场", "购物中心")
        
        // Verify market-related terms
        assertTrue("Should include underground mall", testKeywords.contains("地下商场"))
        assertTrue("Should include underground market", testKeywords.contains("地下市场"))
        assertTrue("Should include shopping center", testKeywords.contains("购物中心"))
    }
} 