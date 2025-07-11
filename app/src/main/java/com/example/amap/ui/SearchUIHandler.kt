package com.example.amap.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView

class SearchUIHandler(
    private val searchEditText: EditText,
    private val onSearch: (String) -> Unit,
    private val onTextChanged: ((String) -> Unit)? = null,
    private val aiProcessingIndicator: TextView? = null,
    private val searchContainer: CardView? = null
) {

    private val inputMethodManager = searchEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    fun setupSearchListeners() {
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Add text change listener for clear button
        onTextChanged?.let { callback ->
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    callback(s?.toString() ?: "")
                }
                
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        if (isValidQuery(query)) {
            hideKeyboard()
            onSearch(query)
        }
    }

    fun hideKeyboard() {
        inputMethodManager.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    fun isValidQuery(query: String): Boolean {
        return query.trim().isNotEmpty()
    }

    fun getCurrentQuery(): String {
        return searchEditText.text.toString().trim()
    }

    fun clearSearch() {
        searchEditText.text.clear()
    }
    
    /**
     * Show AI processing indicator
     */
    fun showAIProcessing() {
        aiProcessingIndicator?.let { indicator ->
            indicator.text = "🤖 AI is processing your query..."
            indicator.visibility = android.view.View.VISIBLE
            
            // Add subtle animation to search container
            searchContainer?.let { container ->
                container.alpha = 0.8f
                container.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .start()
            }
        }
    }
    
    /**
     * Hide AI processing indicator
     */
    fun hideAIProcessing() {
        aiProcessingIndicator?.let { indicator ->
            indicator.visibility = android.view.View.GONE
            
            // Stop animation
            searchContainer?.let { container ->
                container.animate().cancel()
                container.alpha = 1f
            }
        }
    }
    
    /**
     * Show AI processing result info
     */
    fun showAIResultInfo(info: String) {
        aiProcessingIndicator?.let { indicator ->
            indicator.text = "✨ $info"
            indicator.visibility = android.view.View.VISIBLE
            
            // Auto-hide after 3 seconds
            indicator.postDelayed({
                if (indicator.visibility == android.view.View.VISIBLE) {
                    indicator.visibility = android.view.View.GONE
                }
            }, 3000)
        }
    }
} 