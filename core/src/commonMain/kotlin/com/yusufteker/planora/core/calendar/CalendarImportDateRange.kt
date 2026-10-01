package com.yusufteker.planora.core.calendar

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.calendar_import_range_all
import planora.core.generated.resources.calendar_import_range_past_and_future
import planora.core.generated.resources.calendar_import_range_past_month
import planora.core.generated.resources.calendar_import_range_past_week
import planora.core.generated.resources.calendar_import_range_year

/**
 * Predefined date range filters for querying events from native system calendars
 * (Google Calendar, Apple Calendar, etc.).
 *
 * Supported ranges:
 * - [PAST_WEEK]: Events in the last 7 days through the end of today.
 * - [PAST_MONTH]: Events in the last 30 days through the end of today.
 * - [PAST_AND_FUTURE_30_DAYS]: 30 days before and 30 days after the current moment.
 * - [THIS_YEAR]: From the beginning of January 1 to December 31 of the current year.
 * - [ALL]: All events covering a broad 10-year past and 10-year future window.
 *
 * @property labelRes Localized string resource describing the filter range.
 */
enum class CalendarImportDateRange(val labelRes: StringResource) {
    PAST_WEEK(Res.string.calendar_import_range_past_week),
    PAST_MONTH(Res.string.calendar_import_range_past_month),
    PAST_AND_FUTURE_30_DAYS(Res.string.calendar_import_range_past_and_future),
    THIS_YEAR(Res.string.calendar_import_range_year),
    ALL(Res.string.calendar_import_range_all);

    /**
     * Computes the [Pair] of [startEpochMillis, endEpochMillis] representing the start and end of this date range.
     *
     * @param nowMs Current reference timestamp in epoch milliseconds (defaults to system time).
     * @return A pair where `first` is the start timestamp in epoch millis and `second` is the end timestamp in epoch millis.
     */
    fun getEpochRange(nowMs: Long = com.yusufteker.planora.core.utils.getCurrentTimeMs()): Pair<Long, Long> {
        val oneDayMs = 86_400_000L
        return when (this) {
            PAST_WEEK -> {
                try {
                    val tz = TimeZone.currentSystemDefault()
                    val currentDateTime = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)
                    val endOfDay = LocalDateTime(currentDateTime.year, currentDateTime.monthNumber, currentDateTime.dayOfMonth, 23, 59, 59)
                    val endMs = endOfDay.toInstant(tz).toEpochMilliseconds()
                    Pair(endMs - 7L * oneDayMs + 1_000L, endMs)
                } catch (e: Exception) {
                    Pair(nowMs - 7L * oneDayMs, nowMs)
                }
            }
            PAST_MONTH -> {
                try {
                    val tz = TimeZone.currentSystemDefault()
                    val currentDateTime = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)
                    val endOfDay = LocalDateTime(currentDateTime.year, currentDateTime.monthNumber, currentDateTime.dayOfMonth, 23, 59, 59)
                    val endMs = endOfDay.toInstant(tz).toEpochMilliseconds()
                    Pair(endMs - 30L * oneDayMs + 1_000L, endMs)
                } catch (e: Exception) {
                    Pair(nowMs - 30L * oneDayMs, nowMs)
                }
            }
            PAST_AND_FUTURE_30_DAYS -> Pair(nowMs - 30L * oneDayMs, nowMs + 30L * oneDayMs)
            THIS_YEAR -> {
                try {
                    val tz = TimeZone.currentSystemDefault()
                    val currentDateTime = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)
                    val startOfYear = LocalDateTime(currentDateTime.year, 1, 1, 0, 0, 0)
                    val endOfYear = LocalDateTime(currentDateTime.year, 12, 31, 23, 59, 59)
                    Pair(startOfYear.toInstant(tz).toEpochMilliseconds(), endOfYear.toInstant(tz).toEpochMilliseconds())
                } catch (e: Exception) {
                    Pair(nowMs - 180L * oneDayMs, nowMs + 185L * oneDayMs)
                }
            }
            ALL -> Pair(nowMs - 10L * 365L * oneDayMs, nowMs + 10L * 365L * oneDayMs)
        }
    }
}

