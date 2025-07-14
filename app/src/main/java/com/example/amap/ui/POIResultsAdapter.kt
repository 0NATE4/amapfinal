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
        private val titleText: TextView = itemView.findViewById(R.id.poiTitle)
        private val subtitleText: TextView = itemView.findViewById(R.id.poiSubtitle)
        private val addressText: TextView = itemView.findViewById(R.id.poiAddress)
        private val distanceText: TextView = itemView.findViewById(R.id.poiDistance)

        fun bind(poi: POIDisplayItem) {
            Log.d("POIAdapter", "Binding POI: ${poi.title} (English: ${poi.englishTitle})")
            
            // Display names with clear labels
            if (!poi.englishTitle.isNullOrBlank() && poi.englishTitle != poi.title) {
                // Show English name as primary title
                titleText.text = poi.englishTitle
                // Show Chinese name as subtitle with label
                subtitleText.text = "Original: ${poi.title}"
                subtitleText.visibility = View.VISIBLE
            } else {
                // Show Chinese name only with label
                titleText.text = "Original: ${poi.title}"
                subtitleText.visibility = View.GONE
            }
            
            // Address with clear label (always Chinese as requested)
            addressText.text = "Address: ${poi.address}"
            distanceText.text = poi.distance
            
            Log.d("POIAdapter", "Set primary title: '${titleText.text}'")
            Log.d("POIAdapter", "Set subtitle: '${if (subtitleText.visibility == View.VISIBLE) subtitleText.text else "hidden"}'")
            Log.d("POIAdapter", "Set address: '${addressText.text}'")
            
            itemView.setOnClickListener {
                onItemClick(poi)
            }
        }
    }
} 