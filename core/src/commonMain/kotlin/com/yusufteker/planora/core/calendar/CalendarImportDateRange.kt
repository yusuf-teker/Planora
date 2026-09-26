package com.yusufteker.planora.core.calendar

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.calendar_import_range_next_3_months
import planora.core.generated.resources.calendar_import_range_next_month
import planora.core.generated.resources.calendar_import_range_past_and_future
import planora.core.generated.resources.calendar_import_range_year

/**
 * Predefined date range filters for querying events from native system calendars.
 */
enum class CalendarImportDateRange(val labelRes: StringResource) {
    NEXT_30_DAYS(Res.string.calendar_import_range_next_month),
    NEXT_3_MONTHS(Res.string.calendar_import_range_next_3_months),
    PAST_AND_FUTURE_30_DAYS(Res.string.calendar_import_range_past_and_future),
    THIS_YEAR(Res.string.calendar_import_range_year);

    /**
     * Computes the [startEpochMillis, endEpochMillis] pair for this date range filter.
     */
    fun getEpochRange(nowMs: Long = com.yusufteker.planora.core.utils.getCurrentTimeMs()): Pair<Long, Long> {
        val oneDayMs = 86_400_000L
        return when (this) {
            NEXT_30_DAYS -> Pair(nowMs, nowMs + 30L * oneDayMs)
            NEXT_3_MONTHS -> Pair(nowMs, nowMs + 90L * oneDayMs)
            PAST_AND_FUTURE_30_DAYS -> Pair(nowMs - 30L * oneDayMs, nowMs + 30L * oneDayMs)
            THIS_YEAR -> Pair(nowMs - 90L * oneDayMs, nowMs + 275L * oneDayMs)
        }
    }
}
