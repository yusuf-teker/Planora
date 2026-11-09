package com.yusufteker.planora.core.analytics

/**
 * Bridge between Kotlin Multiplatform and FirebaseCrashlytics on iOS.
 * Swift registers closures at startup so Kotlin can forward crashlytics events and logs.
 */
object IosCrashlyticsBridge {
    /**
     * Sets the user identifier in Firebase Crashlytics.
     */
    var onSetUserId: ((String) -> Unit)? = null

    /**
     * Records a non-fatal error in Firebase Crashlytics.
     *
     * @param name The exception or error class name.
     * @param message The error message or description.
     * @param stackTrace The Kotlin stack trace string.
     */
    var onRecordError: ((name: String, message: String, stackTrace: String) -> Unit)? = null

    /**
     * Appends a log line to the upcoming Crashlytics crash report.
     */
    var onLog: ((String) -> Unit)? = null
}

