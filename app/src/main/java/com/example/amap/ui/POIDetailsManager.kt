package com.example.amap.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.amap.R
import com.example.amap.data.model.POIDisplayItem
import com.example.amap.data.model.POIRichDetails
import com.example.amap.search.POIWebService
import com.example.amap.ui.ReviewsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class POIDetailsManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val apiKey: String,
    private val onDirectionsRequested: ((POIDisplayItem) -> Unit)? = null
) {
    
    private val webService = POIWebService(apiKey)
    private var currentBottomSheet: BottomSheetDialog? = null
    
    companion object {
        private const val TAG = "POIDetailsManager"
    }
    
    fun showPOIDetails(poiDisplayItem: POIDisplayItem) {
        Log.d(TAG, "Showing details for POI: ${poiDisplayItem.title}")
        
        // Create bottom sheet dialog
        val bottomSheet = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_poi_details, null)
        bottomSheet.setContentView(view)
        
        // Set basic info immediately
        populateBasicInfo(view, poiDisplayItem)
        
        // Show loading state
        showLoadingState(view)
        
        // Setup action buttons
        setupActionButtons(view, poiDisplayItem)
        
        bottomSheet.show()
        currentBottomSheet = bottomSheet
        
        // Fetch rich details from Web API
        fetchRichDetails(view, poiDisplayItem)
    }
    
    private fun populateBasicInfo(view: View, poiDisplayItem: POIDisplayItem) {
        view.findViewById<TextView>(R.id.detailTitle).text = poiDisplayItem.title
        
        // Create subtitle with category and address
        val poiItem = poiDisplayItem.poiItem
        val category = poiItem.typeDes?.split(";")?.firstOrNull() ?: "POI"
        val shortAddress = poiDisplayItem.address.split(",").firstOrNull() ?: poiDisplayItem.address
        view.findViewById<TextView>(R.id.detailSubtitle).text = "$category • $shortAddress"
        
        // Set distance in the info section
        view.findViewById<TextView>(R.id.detailDistance).text = poiDisplayItem.distance
    }
    
    private fun showLoadingState(view: View) {
        view.findViewById<View>(R.id.loadingContainer).visibility = View.VISIBLE
        view.findViewById<View>(R.id.errorMessage).visibility = View.GONE
        view.findViewById<View>(R.id.infoHours).visibility = View.GONE
        view.findViewById<View>(R.id.infoRating).visibility = View.GONE
        view.findViewById<View>(R.id.infoCost).visibility = View.GONE
        view.findViewById<View>(R.id.contactContainer).visibility = View.GONE
        view.findViewById<RecyclerView>(R.id.photosRecyclerView).visibility = View.GONE
        view.findViewById<View>(R.id.reviewsLabel).visibility = View.GONE
        view.findViewById<RecyclerView>(R.id.reviewsRecyclerView).visibility = View.GONE
    }
    
    private fun fetchRichDetails(view: View, poiDisplayItem: POIDisplayItem) {
        lifecycleScope.launch {
            try {
                // Try POI ID first
                val poiId = extractPOIId(poiDisplayItem)
                
                val result = if (poiId != null) {
                    Log.d(TAG, "Fetching rich details for POI ID: $poiId")
                    webService.getPOIDetails(poiId)
                } else {
                    // Fallback: search by coordinates and name
                    Log.d(TAG, "No POI ID available, searching by coordinates")
                    val poiItem = poiDisplayItem.poiItem
                    webService.searchNearbyAndMatch(
                        longitude = poiItem.latLonPoint.longitude,
                        latitude = poiItem.latLonPoint.latitude,
                        targetName = poiDisplayItem.title,
                        radius = 50 // Very small radius for exact match
                    )
                }
                
                result.fold(
                    onSuccess = { richDetails ->
                        Log.d(TAG, "Got rich details: ${richDetails.photos.size} photos, rating: ${richDetails.rating}")
                        populateRichDetails(view, richDetails)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to fetch rich details", error)
                        showErrorState(view, "Could not load details: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching rich details", e)
                showErrorState(view, "Network error")
            }
        }
    }
    
    private fun populateRichDetails(view: View, richDetails: POIRichDetails) {
        // Hide loading
        view.findViewById<View>(R.id.loadingContainer).visibility = View.GONE
        
        // Show hours if available
        richDetails.openHours?.let { hours ->
            val hoursContainer = view.findViewById<View>(R.id.infoHours)
            val hoursText = view.findViewById<TextView>(R.id.detailHours)
            hoursText.text = formatOpeningHours(hours)
            hoursContainer.visibility = View.VISIBLE
        }
        
        // Show rating if available
        richDetails.rating?.let { rating ->
            val ratingContainer = view.findViewById<View>(R.id.infoRating)
            val ratingText = view.findViewById<TextView>(R.id.detailRating)
            ratingText.text = "★ $rating"
            ratingContainer.visibility = View.VISIBLE
        }
        
        // Show cost if available (convert to yuan symbols)
        richDetails.cost?.let { cost ->
            val costContainer = view.findViewById<View>(R.id.infoCost)
            val costText = view.findViewById<TextView>(R.id.detailCost)
            setupYuanDisplay(costText, cost)
            costContainer.visibility = View.VISIBLE
        }
        
        // Show phone number if available
        richDetails.telephone?.let { phone ->
            val contactContainer = view.findViewById<View>(R.id.contactContainer)
            val phoneText = view.findViewById<TextView>(R.id.detailPhone)
            phoneText.text = phone
            contactContainer.visibility = View.VISIBLE
        }
        
        // Show photos if available
        if (richDetails.photos.isNotEmpty()) {
            val photosRecyclerView = view.findViewById<RecyclerView>(R.id.photosRecyclerView)
            photosRecyclerView.visibility = View.VISIBLE
            photosRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            photosRecyclerView.adapter = PhotosAdapter(richDetails.photos)
        }
        
        // Show reviews if available
        richDetails.reviews?.let { reviewsList ->
            if (reviewsList.isNotEmpty()) {
                view.findViewById<View>(R.id.reviewsLabel).visibility = View.VISIBLE
                val reviewsRecyclerView = view.findViewById<RecyclerView>(R.id.reviewsRecyclerView)
                reviewsRecyclerView.visibility = View.VISIBLE
                reviewsRecyclerView.layoutManager = LinearLayoutManager(context)
                reviewsRecyclerView.adapter = ReviewsAdapter(reviewsList)
            }
        }
    }
    
    private fun formatOpeningHours(hours: String): String {
        // Just return the time, clean and simple
        return when {
            hours.lowercase().contains("closed") -> "Closed"
            hours.lowercase().contains("24") -> "24 hours"
            hours.contains("-") -> {
                val parts = hours.split("-")
                if (parts.size == 2) {
                    val openTime = formatTime(parts[0].trim())
                    val closeTime = formatTime(parts[1].trim())
                    "$openTime-$closeTime"
                } else {
                    hours
                }
            }
            else -> hours
        }
    }
    
    private fun formatTime(time: String): String {
        // Convert 24-hour time to 12-hour format with PM/AM
        return when {
            time.contains(":") -> {
                val parts = time.split(":")
                if (parts.size >= 2) {
                    val hour = parts[0].toIntOrNull() ?: return time
                    when {
                        hour == 0 -> "12${if (parts.size > 1) ":${parts[1]}" else ""}AM"
                        hour < 12 -> "${hour}${if (parts.size > 1) ":${parts[1]}" else ""}AM"
                        hour == 12 -> "12${if (parts.size > 1) ":${parts[1]}" else ""}PM"
                        else -> "${hour - 12}${if (parts.size > 1) ":${parts[1]}" else ""}PM"
                    }
                } else time
            }
            else -> time
        }
    }
    
    private fun setupYuanDisplay(textView: TextView, cost: String) {
        val yuanLevel = convertToYuanLevel(cost)
        val spannable = SpannableStringBuilder()
        
        // Add active yuan symbols in normal color
        for (i in 1..yuanLevel) {
            spannable.append("¥")
        }
        
        // Add ghost yuan symbols in light gray
        val ghostColor = Color.parseColor("#CCCCCC")
        val startGhost = spannable.length
        for (i in yuanLevel + 1..4) {
            spannable.append("¥")
        }
        val endGhost = spannable.length
        
        if (startGhost < endGhost) {
            spannable.setSpan(
                ForegroundColorSpan(ghostColor),
                startGhost,
                endGhost,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        textView.text = spannable
    }
    
    private fun convertToYuanLevel(cost: String): Int {
        // Extract numbers from cost string and classify based on price ranges
        val numbers = extractNumbers(cost)
        val level = if (numbers.isNotEmpty()) {
            val avgPrice = numbers.average()
            Log.d(TAG, "Cost '$cost' -> numbers: $numbers, avgPrice: $avgPrice")
            when {
                avgPrice < 25 -> 1   // ¥ - Cheap (< 25)
                avgPrice < 80 -> 2   // ¥¥ - Moderate (25-80)
                avgPrice < 300 -> 3  // ¥¥¥ - Expensive (80-300)
                else -> 4            // ¥¥¥¥ - Very expensive (300+)
            }
        } else {
            // Fallback to text analysis
            Log.d(TAG, "Cost '$cost' -> no numbers found, using text analysis")
            when {
                cost.lowercase().contains("cheap") || cost.contains("低") -> 1
                cost.lowercase().contains("moderate") || cost.contains("中") -> 2
                cost.lowercase().contains("expensive") || cost.contains("高") -> 3
                cost.lowercase().contains("very expensive") || cost.contains("非常") -> 4
                cost.length <= 10 -> 1 // Short descriptions are usually cheap
                else -> 2 // Default to moderate
            }
        }
        
        Log.d(TAG, "Final yuan level: $level (${("¥".repeat(level))})")
        return level
    }
    
    private fun extractNumbers(text: String): List<Double> {
        val numbers = mutableListOf<Double>()
        
        // Regex to find numbers (including decimals)
        val regex = "\\d+(?:\\.\\d+)?".toRegex()
        regex.findAll(text).forEach { match ->
            match.value.toDoubleOrNull()?.let { numbers.add(it) }
        }
        
        return numbers
    }
    
    private fun showErrorState(view: View, errorMessage: String) {
        view.findViewById<View>(R.id.loadingContainer).visibility = View.GONE
        val errorText = view.findViewById<TextView>(R.id.errorMessage)
        errorText.text = errorMessage
        errorText.visibility = View.VISIBLE
    }
    
    private fun setupActionButtons(view: View, poiDisplayItem: POIDisplayItem) {
        // Call button
        view.findViewById<View>(R.id.actionCall).setOnClickListener {
            // Placeholder - will implement later
            Log.d(TAG, "Call button clicked")
        }
        
        // Directions button
        view.findViewById<View>(R.id.actionDirections).setOnClickListener {
            openNavigation(poiDisplayItem)
        }
        
        // Website button
        view.findViewById<View>(R.id.actionWebsite).setOnClickListener {
            // Placeholder - will implement later
            Log.d(TAG, "Website button clicked")
        }
        
        // Share button
        view.findViewById<View>(R.id.actionShare).setOnClickListener {
            sharePOI(poiDisplayItem)
        }
    }
    
    private fun openNavigation(poiDisplayItem: POIDisplayItem) {
        // Use internal route planning if callback is provided
        if (onDirectionsRequested != null) {
            Log.d(TAG, "Using internal route planning for ${poiDisplayItem.title}")
            onDirectionsRequested.invoke(poiDisplayItem)
            currentBottomSheet?.dismiss()
            return
        }
        
        // Fallback to external navigation
        val poiItem = poiDisplayItem.poiItem
        val lat = poiItem.latLonPoint.latitude
        val lng = poiItem.latLonPoint.longitude
        
        // Open in default maps app
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${poiDisplayItem.title})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to browser with Google Maps
            val mapsUrl = "https://www.google.com/maps?q=$lat,$lng"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
            context.startActivity(webIntent)
        }
        
        currentBottomSheet?.dismiss()
    }
    
    private fun sharePOI(poiDisplayItem: POIDisplayItem) {
        val poiItem = poiDisplayItem.poiItem
        val lat = poiItem.latLonPoint.latitude
        val lng = poiItem.latLonPoint.longitude
        
        val shareText = buildString {
            append("📍 ${poiDisplayItem.title}\n")
            append("📍 ${poiDisplayItem.address}\n")
            append("🗺️ https://www.google.com/maps?q=$lat,$lng\n")
            append("📍 Distance: ${poiDisplayItem.distance}")
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Location: ${poiDisplayItem.title}")
        }
        
        val chooser = Intent.createChooser(shareIntent, "Share location")
        context.startActivity(chooser)
    }
    
    private fun extractPOIId(poiDisplayItem: POIDisplayItem): String? {
        val poiItem = poiDisplayItem.poiItem
        
        Log.d(TAG, "=== POI DEBUG INFO ===")
        Log.d(TAG, "POI Title: ${poiItem.title}")
        Log.d(TAG, "POI ID: ${poiItem.poiId}")
        Log.d(TAG, "POI TypeCode: ${poiItem.typeCode}")
        Log.d(TAG, "POI TypeDes: ${poiItem.typeDes}")
        Log.d(TAG, "POI AdCode: ${poiItem.adCode}")
        Log.d(TAG, "POI AdName: ${poiItem.adName}")
        Log.d(TAG, "POI CityCode: ${poiItem.cityCode}")
        Log.d(TAG, "POI CityName: ${poiItem.cityName}")
        Log.d(TAG, "POI ProvinceCode: ${poiItem.provinceCode}")
        Log.d(TAG, "POI ProvinceName: ${poiItem.provinceName}")
        
        return when {
            !poiItem.poiId.isNullOrBlank() -> {
                Log.d(TAG, "Using poiId: ${poiItem.poiId}")
                poiItem.poiId
            }
            !poiItem.typeCode.isNullOrBlank() -> {
                Log.d(TAG, "Using typeCode as fallback: ${poiItem.typeCode}")
                poiItem.typeCode
            }
            else -> {
                Log.w(TAG, "No usable ID found - will try coordinate-based search instead")
                null
            }
        }
    }
    
    fun dismissCurrentDetails() {
        currentBottomSheet?.dismiss()
        currentBottomSheet = null
    }
} 