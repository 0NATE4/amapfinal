package com.example.amap.ui

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
        poiList.clear()
        poiList.addAll(newResults)
        notifyDataSetChanged()
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
        private val addressText: TextView = itemView.findViewById(R.id.poiAddress)

        fun bind(poi: POIDisplayItem) {
            titleText.text = poi.title
            addressText.text = poi.address
            
            itemView.setOnClickListener {
                onItemClick(poi)
            }
        }
    }
} 