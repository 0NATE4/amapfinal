package com.example.amap.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.amap.R

class TagsAdapter(private val tags: List<String>) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount(): Int = tags.size

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagText: TextView = itemView.findViewById(R.id.tagText)

        fun bind(tag: String) {
            tagText.text = tag
        }
    }
} 