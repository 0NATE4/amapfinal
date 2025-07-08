package com.example.amap.ui

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText

class SearchUIHandler(
    private val searchEditText: EditText,
    private val nearbyButton: Button,
    private val onSearch: (String) -> Unit,
    private val onNearbySearch: () -> Unit
) {

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
        
        nearbyButton.setOnClickListener {
            onNearbySearch()
        }
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        if (isValidQuery(query)) {
            onSearch(query)
        }
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
} 