package com.example.amap.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.amap.R
import com.example.amap.data.model.POIReview

class ReviewsAdapter(private val reviews: List<POIReview>) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val reviewContent: TextView = itemView.findViewById(R.id.reviewContent)
        private val reviewRating: TextView = itemView.findViewById(R.id.reviewRating)
        private val reviewAuthor: TextView = itemView.findViewById(R.id.reviewAuthor)

        fun bind(review: POIReview) {
            reviewContent.text = review.content
            
            // Show rating if available
            review.rating?.let { rating ->
                reviewRating.text = "★ $rating"
                reviewRating.visibility = View.VISIBLE
            } ?: run {
                reviewRating.visibility = View.GONE
            }
            
            // Show author if available
            review.author?.let { author ->
                reviewAuthor.text = "- $author"
                reviewAuthor.visibility = View.VISIBLE
            } ?: run {
                reviewAuthor.visibility = View.GONE
            }
        }
    }
} 