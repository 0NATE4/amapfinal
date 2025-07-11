package com.example.amap.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.amap.R
import com.example.amap.data.model.POIDisplayItem

class POIResultsAdapter(
    private val onItemClick: (POIDisplayItem) -> Unit
) : RecyclerView.Adapter<POIResultsAdapter.POIViewHolder>() {

    private var poiList = mutableListOf<POIDisplayItem>()

    fun updateResults(newResults: List<POIDisplayItem>) {
        Log.d("POIAdapter", "Updating results with ${newResults.size} items")
        poiList.clear()
        poiList.addAll(newResults)
        Log.d("POIAdapter", "Adapter list now has ${poiList.size} items")
        notifyDataSetChanged()
        Log.d("POIAdapter", "notifyDataSetChanged() called")
    }

    fun clearResults() {
        poiList.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poi_result, parent, false)
        return POIViewHolder(view)
    }

    override fun onBindViewHolder(holder: POIViewHolder, position: Int) {
        holder.bind(poiList[position])
    }

    override fun getItemCount(): Int = poiList.size

    inner class POIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleEnglishText: TextView = itemView.findViewById(R.id.poiTitleEnglish)
        private val titleChineseText: TextView = itemView.findViewById(R.id.poiTitleChinese)
        private val addressText: TextView = itemView.findViewById(R.id.poiAddress)
        private val distanceText: TextView = itemView.findViewById(R.id.poiDistance)

        fun bind(poi: POIDisplayItem) {
            Log.d("POIAdapter", "Binding POI: ${poi.title} (English: ${poi.englishTitle})")
            
            // Display English translation prominently (if available)
            titleEnglishText.text = poi.englishTitle ?: poi.title
            
            // Display Chinese name below (only if we have English translation)
            if (!poi.englishTitle.isNullOrBlank() && poi.englishTitle != poi.title) {
                titleChineseText.text = poi.title
                titleChineseText.visibility = View.VISIBLE
            } else {
                // If no English translation, hide Chinese subtitle to avoid duplication
                titleChineseText.visibility = View.GONE
            }
            
            // Address stays in Chinese
            addressText.text = poi.address
            distanceText.text = poi.distance
            
            Log.d("POIAdapter", "Set English title: '${poi.englishTitle}'")
            Log.d("POIAdapter", "Set Chinese title: '${poi.title}'")
            Log.d("POIAdapter", "Set address: '${poi.address}'")
            
            itemView.setOnClickListener {
                onItemClick(poi)
            }
        }
    }
} 