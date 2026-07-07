package com.yusufteker.pulse.shared

actual fun getPlatformName(): String = "Android"

actual fun isEmulator(): Boolean {
    return android.os.Build.FINGERPRINT.contains("generic") ||
           android.os.Build.MODEL.contains("Emulator") ||
           android.os.Build.MODEL.contains("Android SDK built for x86")
}
