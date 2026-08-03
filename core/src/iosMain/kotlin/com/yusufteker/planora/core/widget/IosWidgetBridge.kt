package com.yusufteker.planora.core.widget

import com.yusufteker.planora.core.database.PlanoraDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.localeIdentifier
import platform.Foundation.timeIntervalSince1970

/**
 * iOS Widget Bridge for Planora.
 * Exports localized tasks and events into NSUserDefaults (App Group & Standard) for WidgetKit.
 */
object IosWidgetBridge : KoinComponent {

    private val database: PlanoraDatabase by inject()
    private const val SUITE_NAME = "group.com.yusufteker.planora"
    private const val WIDGET_DATA_KEY = "widget_data_json"

    var onWidgetDataReloadRequested: (() -> Unit)? = null

    fun syncWidgetData() {
        try {
            val db = database
            val entities = db.planoraDatabaseQueries.getAllTasks().executeAsList()

            val now = NSDate()
            val localeId = NSLocale.currentLocale.localeIdentifier
            val isTurkish = localeId.startsWith("tr")

            val todayHeader = if (isTurkish) "Bugün" else "Today"
            val weekHeader = if (isTurkish) "Bu Hafta" else "This Week"
            val tabToday = if (isTurkish) "Bugün" else "Today"
            val tabWeek = if (isTurkish) "Hafta" else "Week"
            val noTasksText = if (isTurkish) "Planlanmış görev yok" else "No tasks scheduled"
            val allDayText = if (isTurkish) "Tüm Gün" else "All Day"

            val dateFormatter = NSDateFormatter().apply {
                dateFormat = "yyyy-MM-dd"
                locale = NSLocale.currentLocale
                timeZone = NSTimeZone.localTimeZone
            }

            val todayStr = dateFormatter.stringFromDate(now)

            val subFormatter = NSDateFormatter().apply {
                dateFormat = "d MMMM, EEEE"
                locale = NSLocale.currentLocale
                timeZone = NSTimeZone.localTimeZone
            }
            val todayDateSub = subFormatter.stringFromDate(now)

            val todayItems = mutableListOf<String>()
            val weekItems = mutableListOf<String>()

            val regexDeadline = """"deadline"\s*:\s*(\d+)""".toRegex()

            for (entity in entities) {
                if (entity.type == "NOTE" || entity.type == "FOLDER" || entity.parentId != null) continue

                var actualStartTime = entity.startTime
                var actualEndTime = entity.endTime ?: entity.startTime

                if (entity.type == "TASK" && entity.specificDetails != null) {
                    try {
                        val json = entity.specificDetails
                        val match = regexDeadline.find(json)
                        val deadline = match?.groupValues?.get(1)?.toLongOrNull()
                        if (deadline != null && deadline > 0) {
                            actualStartTime = deadline
                            actualEndTime = deadline
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                // Handle both millisecond (> 10 billion) and second timestamps
                val startSeconds = if (actualStartTime > 10_000_000_000L) actualStartTime / 1000.0 else actualStartTime.toDouble()
                val endSeconds = if (actualEndTime > 10_000_000_000L) actualEndTime / 1000.0 else actualEndTime.toDouble()

                val startDate = NSDate.dateWithTimeIntervalSince1970(startSeconds)
                val endDate = NSDate.dateWithTimeIntervalSince1970(endSeconds)

                val itemDateStr = dateFormatter.stringFromDate(startDate)
                val itemEndDateStr = dateFormatter.stringFromDate(endDate)

                val timeFormatter = NSDateFormatter().apply {
                    dateFormat = "HH:mm"
                    locale = NSLocale.currentLocale
                    timeZone = NSTimeZone.localTimeZone
                }

                val startTimeStr = timeFormatter.stringFromDate(startDate)
                val endTimeStr = timeFormatter.stringFromDate(endDate)

                val timeString = if (entity.isAllDay == 1L) {
                    allDayText
                } else if (entity.type == "EVENT" && actualEndTime > actualStartTime) {
                    "$startTimeStr - $endTimeStr"
                } else {
                    startTimeStr
                }

                val isCompleted = entity.status == "COMPLETED"
                val escapedTitle = entity.title
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", "")

                val itemJson = "{\"id\":\"${entity.id}\",\"title\":\"$escapedTitle\",\"timeString\":\"$timeString\",\"type\":\"${entity.type}\",\"isCompleted\":$isCompleted}"

                // Match today
                val isToday = (todayStr >= itemDateStr && todayStr <= itemEndDateStr) || (itemDateStr == todayStr)
                if (isToday) {
                    todayItems.add(itemJson)
                }

                // Match current week (-1 to +7 days)
                val diffDays = (startSeconds - now.timeIntervalSince1970) / (60.0 * 60.0 * 24.0)
                if (diffDays >= -1.0 && diffDays <= 7.0) {
                    val dayNameFormatter = NSDateFormatter().apply {
                        dateFormat = "EEE"
                        locale = NSLocale.currentLocale
                        timeZone = NSTimeZone.localTimeZone
                    }
                    val dayName = dayNameFormatter.stringFromDate(startDate)
                    val weekItemJson = "{\"id\":\"${entity.id}\",\"title\":\"$escapedTitle\",\"timeString\":\"$dayName • $timeString\",\"type\":\"${entity.type}\",\"isCompleted\":$isCompleted}"
                    weekItems.add(weekItemJson)
                }
            }

            val todayJsonArray = todayItems.joinToString(",", "[", "]")
            val weekJsonArray = weekItems.joinToString(",", "[", "]")

            val fullJson = "{\"todayHeader\":\"$todayHeader\",\"weekHeader\":\"$weekHeader\",\"tabToday\":\"$tabToday\",\"tabWeek\":\"$tabWeek\",\"noTasksText\":\"$noTasksText\",\"todayDateSub\":\"$todayDateSub\",\"todayTasks\":$todayJsonArray,\"weekTasks\":$weekJsonArray}"

            // Save to both Group Defaults and Standard Defaults
            val groupDefaults = NSUserDefaults(suiteName = SUITE_NAME)
            groupDefaults?.setObject(fullJson, forKey = WIDGET_DATA_KEY)
            groupDefaults?.synchronize()

            val stdDefaults = NSUserDefaults.standardUserDefaults
            stdDefaults.setObject(fullJson, forKey = WIDGET_DATA_KEY)
            stdDefaults.synchronize()

            onWidgetDataReloadRequested?.invoke()
        } catch (e: Throwable) {
            println("IosWidgetBridge syncWidgetData error: ${e.message}")
        }
    }
}

