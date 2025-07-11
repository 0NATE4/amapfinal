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
            // Set review content (always visible since we filtered for valid content)
            reviewContent.text = review.content
            
            // Show rating if available and valid
            review.rating?.let { rating ->
                // Validate rating is actually a number
                val numericRating = rating.toDoubleOrNull()
                if (numericRating != null && numericRating > 0 && numericRating <= 5) {
                    reviewRating.text = "★ $rating"
                    reviewRating.visibility = View.VISIBLE
                } else {
                    reviewRating.visibility = View.GONE
                }
            } ?: run {
                reviewRating.visibility = View.GONE
            }
            
            // Show author if available and not just numbers/IDs
            review.author?.let { author ->
                if (author.isNotBlank() && author.length > 2 && !author.matches(Regex("\\d+"))) {
                    reviewAuthor.text = author
                    reviewAuthor.visibility = View.VISIBLE
                } else {
                    reviewAuthor.visibility = View.GONE
                }
            } ?: run {
                reviewAuthor.visibility = View.GONE
            }
        }
    }
} 