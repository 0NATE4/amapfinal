package com.example.amap.ai

import android.location.Location
import android.util.Log
import com.example.amap.core.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class DeepSeekAIService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(Constants.AI.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.AI.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(Constants.AI.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Translates and enhances user query for AMap search
     * Handles English to Chinese translation, pinyin conversion, and natural language processing
     */
    suspend fun processSearchQuery(
        userQuery: String,
        userLocation: Location?
    ): AIProcessedQuery = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(userQuery, userLocation)
            val response = callDeepSeekAPI(prompt)
            parseAIResponse(response, userQuery)
        } catch (e: Exception) {
            Log.e("DeepSeekAI", "Error processing query: ${e.message}", e)
            // Fallback to original query if AI fails
            AIProcessedQuery(
                originalQuery = userQuery,
                translatedQuery = userQuery,
                searchKeywords = listOf(userQuery),
                confidence = 0.0,
                isFallback = true
            )
        }
    }
    
    private fun buildPrompt(userQuery: String, userLocation: Location?): String {
        val locationContext = if (userLocation != null) {
            "User is located at: ${userLocation.latitude}, ${userLocation.longitude}. "
        } else {
            "User location is not available. "
        }
        
        return """
            You are an AI assistant helping foreigners in China find locations using AMap (高德地图).
            
            $locationContext
            
            User query: "$userQuery"
            
            Your task is to:
            1. If the query is in English, translate it to Chinese for AMap API
            2. If the query is in pinyin, convert it to Chinese characters
            3. If the query is natural language, extract relevant search keywords
            4. Generate multiple search keywords that would help find relevant places
            5. Consider the user's location context for relevant searches
            6. For cuisine types, include both the cuisine name and common restaurant terms
            7. For specific places, include both the exact name and related terms
            8. Think like a local person would search for this type of place
            
            Respond in JSON format:
            {
                "translated_query": "Chinese translation or original if already Chinese",
                "search_keywords": ["keyword1", "keyword2", "keyword3"],
                "confidence": 0.95,
                "explanation": "Brief explanation of the translation/processing"
            }
            
            Guidelines:
            - For restaurants: Include cuisine type + "餐厅", "店", "美食"
            - For shopping: Include category + "商场", "店", "购物"
            - For services: Include service type + "服务", "中心", "店"
            - For landmarks: Include both specific name and general category
            - Always provide 3-5 relevant keywords that would help find the place
            
            Examples of smart keyword generation:
            - "korean food" → Think: What would locals search for? ["韩式料理", "韩国料理", "朝鲜族美食", "韩餐"]
            - "coffee shop" → Think: What terms work in China? ["咖啡店", "咖啡", "星巴克", "饮品店"]
            - "underground market" → Think: How do locals describe this? ["地下商场", "地下市场", "购物中心"]
            - "beijing hutong" → Think: Specific + general terms ["北京胡同", "胡同", "老北京"]
            
            IMPORTANT: Generate keywords that would actually work in AMap search, not just direct translations.
        """.trimIndent()
    }
    
    /**
     * Enhanced search with learning capability - can provide context about successful searches
     */
    suspend fun processSearchQueryWithContext(
        userQuery: String,
        userLocation: Location?,
        successfulKeywords: List<String>? = null
    ): AIProcessedQuery = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPromptWithContext(userQuery, userLocation, successfulKeywords)
            val response = callDeepSeekAPI(prompt)
            parseAIResponse(response, userQuery)
        } catch (e: Exception) {
            Log.e("DeepSeekAI", "Error processing query: ${e.message}", e)
            // Fallback to original query if AI fails
            AIProcessedQuery(
                originalQuery = userQuery,
                translatedQuery = userQuery,
                searchKeywords = listOf(userQuery),
                confidence = 0.0,
                isFallback = true
            )
        }
    }
    
    private fun buildPromptWithContext(userQuery: String, userLocation: Location?, successfulKeywords: List<String>?): String {
        val locationContext = if (userLocation != null) {
            "User is located at: ${userLocation.latitude}, ${userLocation.longitude}. "
        } else {
            "User location is not available. "
        }
        
        val learningContext = if (successfulKeywords != null && successfulKeywords.isNotEmpty()) {
            """
            
            Learning Context: Previously successful keywords for similar queries:
            ${successfulKeywords.joinToString(", ")}
            Use this information to improve keyword generation.
            """.trimIndent()
        } else {
            ""
        }
        
        return """
            You are an AI assistant helping foreigners in China find locations using AMap (高德地图).
            
            $locationContext
            
            User query: "$userQuery"$learningContext
            
            Your task is to:
            1. If the query is in English, translate it to Chinese for AMap API
            2. If the query is in pinyin, convert it to Chinese characters
            3. If the query is natural language, extract relevant search keywords
            4. Generate multiple search keywords that would help find relevant places
            5. Consider the user's location context for relevant searches
            6. For cuisine types, include both the cuisine name and common restaurant terms
            7. For specific places, include both the exact name and related terms
            8. Think like a local person would search for this type of place
            9. If learning context is provided, use it to improve keyword selection
            
            Respond in JSON format:
            {
                "translated_query": "Chinese translation or original if already Chinese",
                "search_keywords": ["keyword1", "keyword2", "keyword3"],
                "confidence": 0.95,
                "explanation": "Brief explanation of the translation/processing"
            }
            
            Guidelines:
            - For restaurants: Include cuisine type + "餐厅", "店", "美食"
            - For shopping: Include category + "商场", "店", "购物"
            - For services: Include service type + "服务", "中心", "店"
            - For landmarks: Include both specific name and general category
            - Always provide 3-5 relevant keywords that would help find the place
            
            Examples of smart keyword generation:
            - "korean food" → Think: What would locals search for? ["韩式料理", "韩国料理", "朝鲜族美食", "韩餐"]
            - "coffee shop" → Think: What terms work in China? ["咖啡店", "咖啡", "星巴克", "饮品店"]
            - "underground market" → Think: How do locals describe this? ["地下商场", "地下市场", "购物中心"]
            - "beijing hutong" → Think: Specific + general terms ["北京胡同", "胡同", "老北京"]
            
            IMPORTANT: Generate keywords that would actually work in AMap search, not just direct translations.
        """.trimIndent()
    }
    
    suspend fun callDeepSeekAPI(prompt: String): String {
        val requestBody = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", Constants.AI.MAX_TOKENS)
            put("temperature", Constants.AI.TEMPERATURE)
        }.toString()
        
        Log.d("DeepSeekAI", "Sending request to DeepSeek API...")
        Log.d("DeepSeekAI", "Request body: $requestBody")
        
        val request = Request.Builder()
            .url("${Constants.ApiKeys.DEEPSEEK_BASE_URL}chat/completions")
            .addHeader("Authorization", "Bearer ${Constants.ApiKeys.DEEPSEEK_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody(mediaType))
            .build()
        
        return client.newCall(request).execute().use { response ->
            Log.d("DeepSeekAI", "Response code: ${response.code}")
            Log.d("DeepSeekAI", "Response headers: ${response.headers}")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e("DeepSeekAI", "API call failed: ${response.code}, Error: $errorBody")
                throw Exception("API call failed: ${response.code}, Error: $errorBody")
            }
            
            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            Log.d("DeepSeekAI", "Raw API response: $responseBody")
            responseBody
        }
    }
    
    private fun parseAIResponse(response: String, originalQuery: String): AIProcessedQuery {
        try {
            Log.d("DeepSeekAI", "Parsing AI response...")
            val jsonResponse = JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content")
            
            Log.d("DeepSeekAI", "AI content: $content")
            
            // Parse the AI response content
            val aiResponse = JSONObject(content)
            
            val translatedQuery = aiResponse.getString("translated_query")
            val searchKeywords = aiResponse.getJSONArray("search_keywords").let { array ->
                (0 until array.length()).map { array.getString(it) }
            }
            val confidence = aiResponse.getDouble("confidence")
            val explanation = aiResponse.getString("explanation")
            
            Log.d("DeepSeekAI", "Parsed - translated_query: $translatedQuery")
            Log.d("DeepSeekAI", "Parsed - search_keywords: $searchKeywords")
            Log.d("DeepSeekAI", "Parsed - confidence: $confidence")
            Log.d("DeepSeekAI", "Parsed - explanation: $explanation")
            
            return AIProcessedQuery(
                originalQuery = originalQuery,
                translatedQuery = translatedQuery,
                searchKeywords = searchKeywords,
                confidence = confidence,
                explanation = explanation,
                isFallback = false
            )
        } catch (e: Exception) {
            Log.e("DeepSeekAI", "Error parsing AI response: ${e.message}", e)
            Log.e("DeepSeekAI", "Response that failed to parse: $response")
            throw e
        }
    }

    /**
     * Translate a single phrase (e.g., address) to English using AI
     */
    suspend fun translateToEnglish(text: String): String = withContext(Dispatchers.IO) {
        try {
            val prompt = "Translate the following Chinese address to natural English. Only provide the English translation, nothing else:\n\n$text"
            val response = callDeepSeekAPI(prompt)
            val jsonResponse = org.json.JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content")
            content.trim()
        } catch (e: Exception) {
            text // Fallback to original if translation fails
        }
    }

    /**
     * Translates a list of business names from Chinese to English in a single API call
     * This is more efficient than individual translations and maintains consistency
     */
    suspend fun translateBusinessNames(businessNames: List<String>): List<String> = withContext(Dispatchers.IO) {
        if (businessNames.isEmpty()) return@withContext emptyList()
        
        try {
            Log.d("DeepSeekAI", "Translating ${businessNames.size} business names")
            
            val prompt = buildTranslationPrompt(businessNames)
            val response = callDeepSeekAPI(prompt)
            parseTranslationResponse(response, businessNames)
        } catch (e: Exception) {
            Log.e("DeepSeekAI", "Error translating business names: ${e.message}", e)
            // Fallback to original names if translation fails
            businessNames
        }
    }

    private fun buildTranslationPrompt(businessNames: List<String>): String {
        val businessNamesJson = JSONArray(businessNames).toString()
        
        return """
            Translate the following Chinese business names to English. These are names of places, restaurants, shops, and other businesses from Chinese maps.
            
            Rules for translation:
            1. Provide natural English translations that sound good to English speakers
            2. Keep proper nouns (like chain names) if they have official English names
            3. For generic descriptions, translate to common English terms
            4. If unsure, provide a reasonable English equivalent
            5. Return ONLY a JSON array of strings in the same order as input
            6. Do not include any explanations or additional text
            7. If a name is already in English or mixed, keep the English parts and translate Chinese parts
            
            Examples:
            - "麦当劳" → "McDonald's"
            - "星巴克咖啡" → "Starbucks Coffee"
            - "北京烤鸭店" → "Beijing Roast Duck Restaurant"
            - "中国银行" → "Bank of China"
            - "购物中心" → "Shopping Center"
            - "人民医院" → "People's Hospital"
            
            Business names to translate:
            $businessNamesJson
            
            Return only the JSON array of translated names:
        """.trimIndent()
    }

    private fun parseTranslationResponse(response: String, originalNames: List<String>): List<String> {
        try {
            Log.d("DeepSeekAI", "Parsing translation response...")
            val jsonResponse = JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content").trim()
            
            Log.d("DeepSeekAI", "Translation content: $content")
            
            // Parse the JSON array of translations
            val translationsArray = JSONArray(content)
            val translations = mutableListOf<String>()
            
            for (i in 0 until translationsArray.length()) {
                translations.add(translationsArray.getString(i))
            }
            
            // Ensure we have the same number of translations as original names
            if (translations.size != originalNames.size) {
                Log.w("DeepSeekAI", "Translation count mismatch: expected ${originalNames.size}, got ${translations.size}")
                return originalNames // Fallback to original names
            }
            
            Log.d("DeepSeekAI", "Successfully translated ${translations.size} business names")
            return translations
            
        } catch (e: Exception) {
            Log.e("DeepSeekAI", "Error parsing translation response: ${e.message}", e)
            Log.e("DeepSeekAI", "Response that failed to parse: $response")
            return originalNames // Fallback to original names
        }
    }
}

/**
 * Data class representing the processed query from AI
 */
data class AIProcessedQuery(
    val originalQuery: String,
    val translatedQuery: String,
    val searchKeywords: List<String>,
    val confidence: Double,
    val explanation: String = "",
    val isFallback: Boolean = false
) 