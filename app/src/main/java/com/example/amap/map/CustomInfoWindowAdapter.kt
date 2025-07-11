package com.example.amap.map

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.amap.api.maps.AMap
import com.amap.api.maps.model.Marker
import com.example.amap.R

class CustomInfoWindowAdapter(private val context: Context) : AMap.InfoWindowAdapter {
    
    override fun getInfoWindow(marker: Marker): View {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
        
        // Populate info window with marker data
        val title = marker.title ?: "Unknown Location"
        val snippet = marker.snippet ?: ""
        
        view.findViewById<TextView>(R.id.infoTitle).text = title
        view.findViewById<TextView>(R.id.infoAddress).text = extractAddress(snippet)
        
        // Extract and display category and distance
        val (category, distance) = extractCategoryAndDistance(title, snippet)
        
        val categoryView = view.findViewById<TextView>(R.id.infoCategory)
        val distanceView = view.findViewById<TextView>(R.id.infoDistance)
        
        if (category.isNotEmpty()) {
            categoryView.text = category
            categoryView.background = getCategoryBackground(category)
            categoryView.visibility = View.VISIBLE
        } else {
            categoryView.visibility = View.GONE
        }
        
        if (distance.isNotEmpty()) {
            distanceView.text = distance
            distanceView.visibility = View.VISIBLE
        } else {
            distanceView.visibility = View.GONE
        }
        
        return view
    }
    
    override fun getInfoContents(marker: Marker): View? {
        // Return null to use custom window instead of just custom contents
        return null
    }
    
    private fun extractAddress(snippet: String): String {
        // Extract address part (before the distance indicator "•")
        return if (snippet.contains("•")) {
            snippet.substringBefore("•").trim()
        } else {
            snippet.take(100) // Limit address length
        }
    }
    
    private fun extractCategoryAndDistance(title: String, snippet: String): Pair<String, String> {
        var category = ""
        var distance = ""
        
        // Extract distance (after "•" in snippet)
        if (snippet.contains("•")) {
            val parts = snippet.split("•")
            if (parts.size > 1) {
                val distancePart = parts[1].trim()
                if (distancePart.contains("away")) {
                    distance = distancePart.replace("away", "").trim()
                } else {
                    distance = distancePart
                }
            }
        }
        
        // Determine category from title and snippet content
        val searchText = "$title $snippet".lowercase()
        category = when {
            searchText.contains("餐") || searchText.contains("restaurant") ||
            searchText.contains("food") || searchText.contains("咖啡") ||
            searchText.contains("coffee") || searchText.contains("饭店") ||
            searchText.contains("麦当劳") || searchText.contains("肯德基") -> "Restaurant"
            
            searchText.contains("购物") || searchText.contains("shopping") ||
            searchText.contains("mall") || searchText.contains("商场") ||
            searchText.contains("超市") || searchText.contains("market") ||
            searchText.contains("店") -> "Shopping"
            
            searchText.contains("娱乐") || searchText.contains("entertainment") ||
            searchText.contains("cinema") || searchText.contains("电影") ||
            searchText.contains("ktv") || searchText.contains("游戏") ||
            searchText.contains("酒吧") -> "Entertainment"
            
            searchText.contains("交通") || searchText.contains("transport") ||
            searchText.contains("station") || searchText.contains("地铁") ||
            searchText.contains("公交") || searchText.contains("火车") ||
            searchText.contains("机场") || searchText.contains("airport") ||
            searchText.contains("停车") -> "Transport"
            
            searchText.contains("医") || searchText.contains("health") ||
            searchText.contains("hospital") || searchText.contains("诊所") ||
            searchText.contains("pharmacy") || searchText.contains("药店") -> "Health"
            
            searchText.contains("教育") || searchText.contains("education") ||
            searchText.contains("school") || searchText.contains("学校") ||
            searchText.contains("大学") || searchText.contains("university") -> "Education"
            
            searchText.contains("银行") || searchText.contains("bank") ||
            searchText.contains("atm") -> "Banking"
            
            searchText.contains("酒店") || searchText.contains("hotel") ||
            searchText.contains("宾馆") -> "Hotel"
            
            else -> "Location"
        }
        
        return Pair(category, distance)
    }
    
    private fun getCategoryBackground(category: String): android.graphics.drawable.GradientDrawable {
        val color = when (category.lowercase()) {
            "restaurant" -> Color.parseColor("#FF5722")
            "shopping" -> Color.parseColor("#9C27B0")
            "entertainment" -> Color.parseColor("#E91E63")
            "transport" -> Color.parseColor("#2196F3")
            "health" -> Color.parseColor("#00BCD4")
            "education" -> Color.parseColor("#FF9800")
            "banking" -> Color.parseColor("#4CAF50")
            "hotel" -> Color.parseColor("#795548")
            else -> Color.parseColor("#607D8B")
        }
        
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = 16f // 8dp radius for rounded chip
        return drawable
    }
} 