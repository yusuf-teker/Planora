package com.yusufteker.planora.core.holiday

import java.util.Locale

actual fun getDeviceCountryCode(): String {
    return try {
        val country = Locale.getDefault().country
        if (country.isNullOrBlank()) "US" else country.uppercase()
    } catch (e: Exception) {
        "US"
    }
}
