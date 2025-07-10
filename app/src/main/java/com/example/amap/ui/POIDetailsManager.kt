package com.example.amap.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class POIDetailsManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val apiKey: String
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
        view.findViewById<TextView>(R.id.detailAddress).text = poiDisplayItem.address
        view.findViewById<TextView>(R.id.detailDistance).text = poiDisplayItem.distance
    }
    
    private fun showLoadingState(view: View) {
        view.findViewById<View>(R.id.loadingContainer).visibility = View.VISIBLE
        view.findViewById<View>(R.id.errorMessage).visibility = View.GONE
        view.findViewById<View>(R.id.ratingCostContainer).visibility = View.GONE
        view.findViewById<View>(R.id.contactContainer).visibility = View.GONE
        view.findViewById<View>(R.id.tagsLabel).visibility = View.GONE
        view.findViewById<RecyclerView>(R.id.tagsRecyclerView).visibility = View.GONE
        view.findViewById<RecyclerView>(R.id.photosRecyclerView).visibility = View.GONE
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
        
        // Show rating and cost if available
        val ratingCostContainer = view.findViewById<View>(R.id.ratingCostContainer)
        val ratingText = view.findViewById<TextView>(R.id.detailRating)
        val costText = view.findViewById<TextView>(R.id.detailCost)
        
        var hasRatingOrCost = false
        
        richDetails.rating?.let { rating ->
            ratingText.text = "★ $rating"
            ratingText.visibility = View.VISIBLE
            hasRatingOrCost = true
        } ?: run {
            ratingText.visibility = View.GONE
        }
        
        richDetails.cost?.let { cost ->
            costText.text = cost
            costText.visibility = View.VISIBLE
            hasRatingOrCost = true
        } ?: run {
            costText.visibility = View.GONE
        }
        
        ratingCostContainer.visibility = if (hasRatingOrCost) View.VISIBLE else View.GONE
        
        // Show contact info if available
        val contactContainer = view.findViewById<View>(R.id.contactContainer)
        val phoneText = view.findViewById<TextView>(R.id.detailPhone)
        val hoursText = view.findViewById<TextView>(R.id.detailHours)
        
        var hasContactInfo = false
        
        richDetails.telephone?.let { phone ->
            phoneText.text = "📞 $phone"
            phoneText.visibility = View.VISIBLE
            hasContactInfo = true
        } ?: run {
            phoneText.visibility = View.GONE
        }
        
        richDetails.openHours?.let { hours ->
            hoursText.text = "🕒 $hours"
            hoursText.visibility = View.VISIBLE
            hasContactInfo = true
        } ?: run {
            hoursText.visibility = View.GONE
        }
        
        contactContainer.visibility = if (hasContactInfo) View.VISIBLE else View.GONE
        
        // Show tags if available
        if (richDetails.tags.isNotEmpty()) {
            view.findViewById<View>(R.id.tagsLabel).visibility = View.VISIBLE
            val tagsRecyclerView = view.findViewById<RecyclerView>(R.id.tagsRecyclerView)
            tagsRecyclerView.visibility = View.VISIBLE
            tagsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            tagsRecyclerView.adapter = TagsAdapter(richDetails.tags)
        }
        
        // Show photos if available
        if (richDetails.photos.isNotEmpty()) {
            val photosRecyclerView = view.findViewById<RecyclerView>(R.id.photosRecyclerView)
            photosRecyclerView.visibility = View.VISIBLE
            photosRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            photosRecyclerView.adapter = PhotosAdapter(richDetails.photos)
        }
    }
    
    private fun showErrorState(view: View, errorMessage: String) {
        view.findViewById<View>(R.id.loadingContainer).visibility = View.GONE
        val errorText = view.findViewById<TextView>(R.id.errorMessage)
        errorText.text = errorMessage
        errorText.visibility = View.VISIBLE
    }
    
    private fun setupActionButtons(view: View, poiDisplayItem: POIDisplayItem) {
        val navigateButton = view.findViewById<MaterialButton>(R.id.navigateButton)
        val shareButton = view.findViewById<MaterialButton>(R.id.shareButton)
        
        navigateButton.setOnClickListener {
            openNavigation(poiDisplayItem)
        }
        
        shareButton.setOnClickListener {
            sharePOI(poiDisplayItem)
        }
    }
    
    private fun openNavigation(poiDisplayItem: POIDisplayItem) {
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