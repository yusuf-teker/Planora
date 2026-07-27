package com.yusufteker.planora.core.holiday

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.countryCode

actual fun getDeviceCountryCode(): String {
    return try {
        val country = NSLocale.currentLocale.countryCode
        if (country.isNullOrBlank()) "US" else country.uppercase()
    } catch (e: Exception) {
        "US"
    }
}
