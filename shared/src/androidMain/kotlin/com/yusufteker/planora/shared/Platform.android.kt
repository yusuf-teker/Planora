package com.yusufteker.planora.shared

import android.os.Build

actual fun getPlatformName(): String = "Android"

actual fun isEmulator(): Boolean {

    val fingerprint = Build.FINGERPRINT
    val model = Build.MODEL
    val manufacturer = Build.MANUFACTURER
    val brand = Build.BRAND
    val device = Build.DEVICE
    val product = Build.PRODUCT
    val hardware = Build.HARDWARE

    return (
            fingerprint.startsWith("generic") ||
                    fingerprint.startsWith("unknown") ||
                    fingerprint.contains("test-keys") ||
                    model.contains("google_sdk") ||
                    model.contains("Emulator") ||
                    model.contains("Android SDK built for") ||
                    manufacturer.contains("Genymotion") ||
                    (brand.startsWith("generic") && device.startsWith("generic")) ||
                    product == "google_sdk" ||
                    hardware.contains("goldfish") ||   // klasik Android emulator (QEMU tabanlı)
                    hardware.contains("ranchu") ||     // yeni nesil Android emulator
                    product.contains("sdk_gphone")     // Android Studio "Pixel" emülatör imajları
            )
}
