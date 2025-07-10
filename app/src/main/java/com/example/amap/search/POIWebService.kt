package com.example.amap.search

import android.util.Log
import com.example.amap.data.model.POIPhoto
import com.example.amap.data.model.POIRichDetails
import com.example.amap.data.model.POIReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class POIWebService(private val apiKey: String) {

    companion object {
        private const val BASE_URL = "https://restapi.amap.com/v5/place"
        private const val TAG = "POIWebService"
    }

    // Search POIs around location with basic info
    suspend fun searchNearby(
        longitude: Double,
        latitude: Double,
        keyword: String = "",
        radius: Int = 1000,
        types: String = ""
    ): Result<List<BasicPOI>> = withContext(Dispatchers.IO) {
        try {
            val location = "$longitude,$latitude"
            val url = "$BASE_URL/around?" +
                    "location=${URLEncoder.encode(location, "UTF-8")}" +
                    "&radius=$radius" +
                    "&keywords=${URLEncoder.encode(keyword, "UTF-8")}" +
                    "&types=${URLEncoder.encode(types, "UTF-8")}" +
                    "&show_fields=business,children" +
                    "&page_size=20" +
                    "&key=$apiKey"

            Log.d(TAG, "Searching nearby: $url")
            
            val response = makeHttpRequest(url)
            val jsonResponse = JSONObject(response)
            
            if (jsonResponse.getString("status") == "1") {
                val pois = jsonResponse.getJSONArray("pois")
                val results = mutableListOf<BasicPOI>()
                
                for (i in 0 until pois.length()) {
                    val poi = pois.getJSONObject(i)
                    results.add(parseBasicPOI(poi))
                }
                
                Log.d(TAG, "Found ${results.size} POIs")
                Result.success(results)
            } else {
                val error = jsonResponse.optString("info", "Unknown error")
                Log.e(TAG, "Search failed: $error")
                Result.failure(Exception("Search failed: $error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            Result.failure(e)
        }
    }

    // Get rich details for a specific POI
    suspend fun getPOIDetails(poiId: String): Result<POIRichDetails> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/detail?" +
                    "id=${URLEncoder.encode(poiId, "UTF-8")}" +
                    "&show_fields=photos,business,children,indoor,navi" +
                    "&key=$apiKey"

            Log.d(TAG, "Fetching POI details: $url")
            
            val response = makeHttpRequest(url)
            Log.d(TAG, "POI Details Response: $response")
            val jsonResponse = JSONObject(response)
            
            if (jsonResponse.getString("status") == "1") {
                val poisArray = jsonResponse.getJSONArray("pois")
                if (poisArray.length() > 0) {
                    val poi = poisArray.getJSONObject(0)
                    val richDetails = parseRichDetails(poi, poiId)
                    Log.d(TAG, "Got rich details for POI: $poiId")
                    Result.success(richDetails)
                } else {
                    Result.failure(Exception("POI not found"))
                }
            } else {
                val error = jsonResponse.optString("info", "Unknown error")
                Log.e(TAG, "Details fetch failed: $error")
                Result.failure(Exception("Details fetch failed: $error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Details fetch error", e)
            Result.failure(e)
        }
    }

    // Search nearby and find best match by name (fallback when POI ID not available)
    suspend fun searchNearbyAndMatch(
        longitude: Double,
        latitude: Double,
        targetName: String,
        radius: Int = 100
    ): Result<POIRichDetails> = withContext(Dispatchers.IO) {
        try {
            val location = "$longitude,$latitude"
            val url = "$BASE_URL/around?" +
                    "location=${URLEncoder.encode(location, "UTF-8")}" +
                    "&radius=$radius" +
                    "&show_fields=photos,business,children,indoor,navi" +
                    "&page_size=20" +
                    "&key=$apiKey"

            Log.d(TAG, "Searching nearby for '$targetName': $url")
            
            val response = makeHttpRequest(url)
            Log.d(TAG, "Nearby Search Response: $response")
            val jsonResponse = JSONObject(response)
            
            if (jsonResponse.getString("status") == "1") {
                val pois = jsonResponse.getJSONArray("pois")
                
                // Find best match by name similarity
                var bestMatch: JSONObject? = null
                var bestScore = 0.0
                
                for (i in 0 until pois.length()) {
                    val poi = pois.getJSONObject(i)
                    val poiName = poi.getString("name")
                    val similarity = calculateNameSimilarity(targetName, poiName)
                    
                    Log.d(TAG, "POI: '$poiName' vs '$targetName' similarity: $similarity")
                    
                    if (similarity > bestScore) {
                        bestScore = similarity
                        bestMatch = poi
                    }
                }
                
                if (bestMatch != null && bestScore > 0.3) { // 30% similarity threshold
                    val richDetails = parseRichDetails(bestMatch, bestMatch.getString("id"))
                    Log.d(TAG, "Found best match with ${bestScore * 100}% similarity")
                    Result.success(richDetails)
                } else {
                    Log.w(TAG, "No good match found (best score: $bestScore)")
                    Result.failure(Exception("No matching POI found nearby"))
                }
            } else {
                val error = jsonResponse.optString("info", "Unknown error")
                Log.e(TAG, "Nearby search failed: $error")
                Result.failure(Exception("Nearby search failed: $error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Nearby search error", e)
            Result.failure(e)
        }
    }

    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    return reader.readText()
                }
            } else {
                throw Exception("HTTP error: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseBasicPOI(poi: JSONObject): BasicPOI {
        val location = poi.getString("location").split(",")
        return BasicPOI(
            id = poi.getString("id"),
            name = poi.getString("name"),
            address = poi.optString("address", ""),
            longitude = location[0].toDouble(),
            latitude = location[1].toDouble(),
            distance = poi.optString("distance", ""),
            type = poi.optString("type", "")
        )
    }

    private fun parseRichDetails(poi: JSONObject, poiId: String): POIRichDetails {
        Log.d(TAG, "Parsing POI details: ${poi.toString()}")
        
        // Parse photos
        val photos = mutableListOf<POIPhoto>()
        poi.optJSONArray("photos")?.let { photosArray ->
            Log.d(TAG, "Found ${photosArray.length()} photos")
            for (i in 0 until photosArray.length()) {
                val photo = photosArray.getJSONObject(i)
                photos.add(POIPhoto(
                    title = photo.optString("title"),
                    url = photo.getString("url")
                ))
            }
        } ?: Log.d(TAG, "No photos array found")

        // Parse tags
        val tags = mutableListOf<String>()
        poi.optString("tag")?.split(";")?.forEach { tag ->
            if (tag.isNotBlank()) tags.add(tag.trim())
        }
        Log.d(TAG, "Found ${tags.size} tags: $tags")

        // Parse reviews/comments
        val reviews = mutableListOf<POIReview>()
        poi.optJSONArray("comments")?.let { commentsArray ->
            Log.d(TAG, "Found ${commentsArray.length()} comments")
            for (i in 0 until commentsArray.length()) {
                val comment = commentsArray.getJSONObject(i)
                reviews.add(POIReview(
                    content = comment.optString("content", ""),
                    rating = comment.optString("rating"),
                    author = comment.optString("author")
                ))
            }
        }
        
        // Try alternative reviews field
        poi.optJSONArray("reviews")?.let { reviewsArray ->
            Log.d(TAG, "Found ${reviewsArray.length()} reviews")
            for (i in 0 until reviewsArray.length()) {
                val review = reviewsArray.getJSONObject(i)
                reviews.add(POIReview(
                    content = review.optString("content", ""),
                    rating = review.optString("star", review.optString("rating")),
                    author = review.optString("author", review.optString("username"))
                ))
            }
        }

        // Parse business information (new API 2.0 structure)
        val businessObj = poi.optJSONObject("business")
        Log.d(TAG, "Business object: ${businessObj?.toString()}")
        
        // Parse rating from business object first, then fallback to root level
        val rating = businessObj?.optString("rating")?.takeIf { it.isNotBlank() }
            ?: poi.optString("rating").takeIf { it.isNotBlank() }
            ?: poi.optString("star").takeIf { it.isNotBlank() }
        
        // Parse cost from business object first, then fallback to root level
        val cost = businessObj?.optString("cost")?.takeIf { it.isNotBlank() }
            ?: poi.optString("cost").takeIf { it.isNotBlank() }
            ?: poi.optString("price_range").takeIf { it.isNotBlank() }
        
        // Parse opening hours from business object (prioritize today's hours)
        val openHoursToday = businessObj?.optString("opentime_today")?.takeIf { it.isNotBlank() }
        val openHoursWeek = businessObj?.optString("opentime_week")?.takeIf { it.isNotBlank() }
        val openHours = openHoursToday ?: openHoursWeek ?: poi.optString("opentime").takeIf { it.isNotBlank() }
        
        // Parse telephone from business object first, then fallback to root level
        val telephone = businessObj?.optString("tel")?.takeIf { it.isNotBlank() }
            ?: poi.optString("tel").takeIf { it.isNotBlank() }
        
        // Parse business area from business object first, then fallback to root level
        val businessArea = businessObj?.optString("business_area")?.takeIf { it.isNotBlank() }
            ?: poi.optString("business_area").takeIf { it.isNotBlank() }
        
        // Parse tag from business object (food POIs have special tags)
        val businessTag = businessObj?.optString("tag")?.takeIf { it.isNotBlank() }
        if (businessTag != null && !tags.contains(businessTag)) {
            tags.add(businessTag)
        }
        
        Log.d(TAG, "Parsed: rating=$rating, cost=$cost, phone=$telephone, openHours=$openHours, reviews=${reviews.size}")
        Log.d(TAG, "Full POI JSON keys: ${poi.keys().asSequence().toList()}")
        Log.d(TAG, "Business object keys: ${businessObj?.keys()?.asSequence()?.toList()}")
        Log.d(TAG, "Final tags: $tags")

        return POIRichDetails(
            id = poiId,
            photos = photos,
            rating = rating,
            cost = cost,
            openHours = openHours,
            telephone = telephone,
            businessArea = businessArea,
            tags = tags,
            reviews = if (reviews.isNotEmpty()) reviews else null
        )
    }

    private fun calculateNameSimilarity(name1: String, name2: String): Double {
        val cleanName1 = name1.lowercase().trim()
        val cleanName2 = name2.lowercase().trim()
        
        // Exact match
        if (cleanName1 == cleanName2) return 1.0
        
        // Contains match
        if (cleanName1.contains(cleanName2) || cleanName2.contains(cleanName1)) return 0.8
        
        // Levenshtein distance similarity
        val distance = levenshteinDistance(cleanName1, cleanName2)
        val maxLength = maxOf(cleanName1.length, cleanName2.length)
        return if (maxLength == 0) 0.0 else 1.0 - (distance.toDouble() / maxLength)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
}

// Basic POI from Web API search
data class BasicPOI(
    val id: String,
    val name: String,
    val address: String,
    val longitude: Double,
    val latitude: Double,
    val distance: String,
    val type: String
) 