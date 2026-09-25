package com.yusufteker.planora.core.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android implementation of [CalendarSyncManager].
 *
 * Utilizes the standard Android [CalendarContract] insert Intent.
 * This launches Google Calendar or the user's default calendar application,
 * pre-populated with title, description, location, and start/end timestamps.
 * Does not require dangerous runtime calendar permissions.
 */
actual class CalendarSyncManager : KoinComponent {

    private val context: Context by inject()

    actual fun addToSystemCalendar(
        title: String,
        description: String?,
        location: String?,
        startTimeEpochMillis: Long,
        endTimeEpochMillis: Long?
    ): Boolean {
        return try {
            val endMillis = endTimeEpochMillis ?: (startTimeEpochMillis + 3600000L) // 1 hour default
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                if (!description.isNullOrBlank()) {
                    putExtra(CalendarContract.Events.DESCRIPTION, description)
                }
                if (!location.isNullOrBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                }
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeEpochMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
