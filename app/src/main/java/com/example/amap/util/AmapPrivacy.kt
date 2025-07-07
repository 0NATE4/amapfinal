package com.example.amap.util

import android.content.Context
import com.amap.api.maps.MapsInitializer

object AmapPrivacy {
    fun ensureCompliance(context: Context) {
        // This keeps the privacy logic isolated and reusable.
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
    }
}