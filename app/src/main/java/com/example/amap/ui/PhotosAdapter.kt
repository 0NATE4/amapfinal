package com.example.amap.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.amap.R
import com.example.amap.data.model.POIPhoto

class PhotosAdapter(private val photos: List<POIPhoto>) : RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoImage: ImageView = itemView.findViewById(R.id.photoImage)
        private val photoTitle: TextView = itemView.findViewById(R.id.photoTitle)

        fun bind(photo: POIPhoto) {
            // Title is hidden in the layout, no need to set it
            
            // Load actual image from URL using Glide
            Glide.with(itemView.context)
                .load(photo.url)
                .placeholder(R.drawable.ic_launcher_foreground) // Show placeholder while loading
                .error(R.drawable.ic_launcher_foreground) // Show fallback if loading fails
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache images for performance
                .centerCrop()
                .into(photoImage)
        }
    }
} 