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
actual class CalendarSyncManager : CalendarService, KoinComponent {

    private val context: Context by inject()

    actual override fun addToSystemCalendar(
        title: String,
        description: String?,
        location: String?,
        startTimeEpochMillis: Long,
        endTimeEpochMillis: Long?
    ): Boolean {
        val endMillis = endTimeEpochMillis ?: (startTimeEpochMillis + 3600000L) // 1 hour default

        // Primary attempt: ACTION_INSERT with CalendarContract.Events.CONTENT_URI
        val insertIntent = Intent(Intent.ACTION_INSERT).apply {
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

        try {
            context.startActivity(insertIntent)
            return true
        } catch (_: Exception) {
            // Secondary attempt: Fallback using ACTION_EDIT and MIME type "vnd.android.cursor.item/event"
            return try {
                val fallbackIntent = Intent(Intent.ACTION_EDIT).apply {
                    type = "vnd.android.cursor.item/event"
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
                context.startActivity(fallbackIntent)
                true
            } catch (e2: Exception) {
                e2.printStackTrace()
                false
            }
        }
    }

    actual override fun hasCalendarReadPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    actual override suspend fun fetchCalendarEvents(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<CalendarImportItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!hasCalendarReadPermission()) {
            return@withContext emptyList()
        }

        val eventsList = mutableListOf<CalendarImportItem>()
        val contentResolver = context.contentResolver

        val builder = android.provider.CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startEpochMillis)
        android.content.ContentUris.appendId(builder, endEpochMillis)

        val projection = arrayOf(
            android.provider.CalendarContract.Instances.EVENT_ID,
            android.provider.CalendarContract.Instances.TITLE,
            android.provider.CalendarContract.Instances.DESCRIPTION,
            android.provider.CalendarContract.Instances.EVENT_LOCATION,
            android.provider.CalendarContract.Instances.BEGIN,
            android.provider.CalendarContract.Instances.END,
            android.provider.CalendarContract.Instances.ALL_DAY,
            android.provider.CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            android.provider.CalendarContract.Events.ACCOUNT_NAME,
            android.provider.CalendarContract.Events.OWNER_ACCOUNT
        )

        try {
            val cursor = contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${android.provider.CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(android.provider.CalendarContract.Instances.EVENT_ID)
                val titleIdx = c.getColumnIndex(android.provider.CalendarContract.Instances.TITLE)
                val descIdx = c.getColumnIndex(android.provider.CalendarContract.Instances.DESCRIPTION)
                val locIdx = c.getColumnIndex(android.provider.CalendarContract.Instances.EVENT_LOCATION)
                val beginIdx = c.getColumnIndex(android.provider.CalendarContract.Instances.BEGIN)
                val endIdx = c.getColumnIndex(android.provider.CalendarContract.Instances.END)
                val allDayIdx = c.getColumnIndex(android.provider.CalendarContract.Instances.ALL_DAY)
                val calNameIdx = c.getColumnIndex(android.provider.CalendarContract.Events.CALENDAR_DISPLAY_NAME)
                val accNameIdx = c.getColumnIndex(android.provider.CalendarContract.Events.ACCOUNT_NAME)
                val ownerIdx = c.getColumnIndex(android.provider.CalendarContract.Events.OWNER_ACCOUNT)

                while (c.moveToNext()) {
                    val calendarName = if (calNameIdx >= 0) c.getString(calNameIdx) else null
                    val accountName = if (accNameIdx >= 0) c.getString(accNameIdx) else null
                    val ownerAccount = if (ownerIdx >= 0) c.getString(ownerIdx) else null

                    // Exclude national/religious holidays, birthday feeds, and system contacts
                    if (isHolidayOrSystemCalendar(calendarName, accountName, ownerAccount)) {
                        continue
                    }

                    val rawTitle = if (titleIdx >= 0) c.getString(titleIdx) else null
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle else "Untitled Event"
                    val eventId = if (idIdx >= 0) c.getLong(idIdx).toString() else com.yusufteker.planora.core.utils.generateUUID()
                    val description = if (descIdx >= 0) c.getString(descIdx) else null
                    val location = if (locIdx >= 0) c.getString(locIdx) else null
                    val begin = if (beginIdx >= 0) c.getLong(beginIdx) else startEpochMillis
                    val end = if (endIdx >= 0) c.getLong(endIdx) else null
                    val isAllDay = if (allDayIdx >= 0) c.getInt(allDayIdx) == 1 else false
                    val targetType = detectTargetType(title, calendarName)

                    eventsList.add(
                        CalendarImportItem(
                            id = eventId,
                            title = title,
                            description = description,
                            location = location,
                            startTimeEpochMillis = begin,
                            endTimeEpochMillis = end,
                            isAllDay = isAllDay,
                            calendarName = calendarName,
                            accountName = accountName,
                            targetType = targetType,
                            priority = com.yusufteker.planora.shared.api.TaskPriority.MEDIUM,
                            isSelected = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        eventsList
    }

    private fun isHolidayOrSystemCalendar(
        calendarName: String?,
        accountName: String?,
        ownerAccount: String?
    ): Boolean {
        val cal = calendarName?.lowercase() ?: ""
        val acc = accountName?.lowercase() ?: ""
        val owner = ownerAccount?.lowercase() ?: ""

        // 1. Google system holiday & contact calendars
        if (acc.contains("holiday@group.v.calendar.google.com") ||
            owner.contains("holiday@group.v.calendar.google.com") ||
            acc.contains("#contacts@group.v.calendar.google.com") ||
            owner.contains("#contacts@group.v.calendar.google.com") ||
            acc.contains("import.calendar.google.com") ||
            owner.contains("import.calendar.google.com")
        ) {
            return true
        }

        // 2. Multi-language keywords for holidays, observances, and birthdays
        val systemKeywords = listOf(
            "holiday", "tatil", "tatilleri", "bayram", "resmi tatil",
            "birthday", "doğum günü", "dogum gunu", "contacts", "rehber"
        )

        return systemKeywords.any { cal.contains(it) || acc.contains(it) || owner.contains(it) }
    }

    private fun detectTargetType(
        title: String,
        calendarName: String?
    ): com.yusufteker.planora.shared.api.TaskType {
        val t = title.lowercase().trim()
        val c = calendarName?.lowercase()?.trim() ?: ""

        val taskKeywords = listOf("task", "tasks", "görev", "görevler", "todo", "to-do", "yapılacak", "reminder", "reminders", "hatırlatıcı")
        if (taskKeywords.any { c.contains(it) }) {
            return com.yusufteker.planora.shared.api.TaskType.TASK
        }

        if (t.startsWith("[ ]") || t.startsWith("[x]") || t.startsWith("todo:") || t.startsWith("görev:") || t.startsWith("task:")) {
            return com.yusufteker.planora.shared.api.TaskType.TASK
        }

        return com.yusufteker.planora.shared.api.TaskType.EVENT
    }
}
