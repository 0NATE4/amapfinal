package com.example.amap

import android.content.Context
import android.util.Log
import com.amap.api.services.core.LatLonPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * RouteController using AMap Web Service API for walking route planning
 * Uses HTTP requests to get route planning data
 */
class RouteController(
    private val context: Context,
    private val onRouteResult: (Boolean, String, RouteResult?) -> Unit
) {

    data class RouteData(
        val distance: Int, // meters
        val duration: Int, // seconds
        val polyline: String // coordinate string
    )
    
    data class RouteResult(
        val routeData: RouteData,
        val startPoint: LatLonPoint,
        val endPoint: LatLonPoint
    )

    /**
     * Plan a walking route between two points using Web Service API
     */
    fun planWalkingRoute(startPoint: LatLonPoint, endPoint: LatLonPoint) {
        Log.d("RouteController", "Planning walking route from ${startPoint.latitude},${startPoint.longitude} to ${endPoint.latitude},${endPoint.longitude}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val origin = "${startPoint.longitude},${startPoint.latitude}"
                val destination = "${endPoint.longitude},${endPoint.latitude}"
                
                // Use the same API key from Constants
                val apiKey = com.example.amap.core.Constants.ApiKeys.WEB_API_KEY
                
                val url = "https://restapi.amap.com/v3/direction/walking?" +
                        "origin=${URLEncoder.encode(origin, "UTF-8")}&" +
                        "destination=${URLEncoder.encode(destination, "UTF-8")}&" +
                        "key=$apiKey"
                
                Log.d("RouteController", "Making request to: $url")
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    
                    Log.d("RouteController", "API Response: $response")
                    parseRouteResponse(response, startPoint, endPoint)
                } else {
                    Log.e("RouteController", "HTTP Error: $responseCode")
                    withContext(Dispatchers.Main) {
                        onRouteResult(false, "HTTP Error: $responseCode", null)
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e("RouteController", "Error planning route: ${e.message}")
                withContext(Dispatchers.Main) {
                    onRouteResult(false, "Route planning failed: ${e.message}", null)
                }
            }
        }
    }
    
    private suspend fun parseRouteResponse(response: String, startPoint: LatLonPoint, endPoint: LatLonPoint) {
        try {
            val json = JSONObject(response)
            val status = json.getString("status")
            
            if (status == "1") {
                val route = json.getJSONObject("route")
                val paths = route.getJSONArray("paths")
                
                if (paths.length() > 0) {
                    val path = paths.getJSONObject(0)
                    val distance = path.getInt("distance")
                    val duration = path.getInt("duration")
                    
                    // Combine all steps' polylines for complete walking route
                    val steps = path.getJSONArray("steps")
                    val allPolylines = mutableListOf<String>()
                    
                    Log.d("RouteController", "Processing ${steps.length()} walking steps")
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val stepPolyline = step.optString("polyline", "")
                        val instruction = step.optString("instruction", "")
                        val distance = step.optInt("distance", 0)
                        
                        Log.d("RouteController", "Step $i: $instruction (${distance}m)")
                        
                        if (stepPolyline.isNotEmpty()) {
                            allPolylines.add(stepPolyline)
                        }
                    }
                    
                    // Combine all polylines with semicolon separator
                    val polyline = allPolylines.joinToString(";")
                    
                    val distanceText = if (distance >= 1000) {
                        "%.1f km".format(distance / 1000.0)
                    } else {
                        "$distance m"
                    }
                    
                    val durationText = if (duration >= 60) {
                        "${duration / 60} min"
                    } else {
                        "$duration sec"
                    }
                    
                    val routeData = RouteData(distance, duration, polyline)
                    val routeResult = RouteResult(routeData, startPoint, endPoint)
                    val message = "Route found: $distanceText, $durationText"
                    
                    Log.d("RouteController", "Route planning successful: $message")
                    
                    withContext(Dispatchers.Main) {
                        onRouteResult(true, message, routeResult)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onRouteResult(false, "No route found", null)
                    }
                }
            } else {
                val info = json.optString("info", "Unknown error")
                Log.e("RouteController", "API Error: $info")
                withContext(Dispatchers.Main) {
                    onRouteResult(false, "Route search failed: $info", null)
                }
            }
        } catch (e: Exception) {
            Log.e("RouteController", "Error parsing response: ${e.message}")
            withContext(Dispatchers.Main) {
                onRouteResult(false, "Error parsing route data: ${e.message}", null)
            }
        }
    }

    fun cleanup() {
        Log.d("RouteController", "RouteController cleaned up")
    }
} 