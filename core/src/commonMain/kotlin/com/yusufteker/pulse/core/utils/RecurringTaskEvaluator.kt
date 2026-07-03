package com.yusufteker.pulse.core.utils

import com.yusufteker.pulse.shared.api.RecurrenceRule
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.daysUntil
import kotlinx.datetime.DateTimePeriod

object RecurringTaskEvaluator {

    /**
     * Given a starting timestamp [startTimeMs] and a [RecurrenceRule],
     * returns a list of specific timestamps (Long) that fall between [rangeStartMs] and [rangeEndMs].
     *
     * Note: This is an "on-the-fly" projector.
     */
    fun generateOccurrences(
        startTimeMs: Long,
        rule: RecurrenceRule,
        rangeStartMs: Long,
        rangeEndMs: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<Long> {
        if (startTimeMs > rangeEndMs) return emptyList()

        val startInstant = Instant.fromEpochMilliseconds(startTimeMs)
        val startDateTime = startInstant.toLocalDateTime(timeZone)
        val startDate = startDateTime.date

        val rangeStartInstant = Instant.fromEpochMilliseconds(rangeStartMs)
        val rangeStartDate = rangeStartInstant.toLocalDateTime(timeZone).date

        val rangeEndInstant = Instant.fromEpochMilliseconds(rangeEndMs)
        val rangeEndDate = rangeEndInstant.toLocalDateTime(timeZone).date

        val occurrences = mutableListOf<Long>()

        // Optimization: start checking from max(startDate, rangeStartDate) to avoid looping from 1970
        var currentDate = if (startDate > rangeStartDate) startDate else rangeStartDate

        // Loop until we exceed rangeEndDate
        while (currentDate <= rangeEndDate) {
            val isOccurring = when (rule) {
                is RecurrenceRule.Daily -> {
                    val daysDiff = startDate.daysUntil(currentDate)
                    daysDiff >= 0 && daysDiff % rule.interval == 0
                }
                is RecurrenceRule.Weekly -> {
                    // Monday is 1, Sunday is 7 (kotlinx.datetime ISO)
                    val dayOfWeekIso = currentDate.dayOfWeek.value
                    startDate <= currentDate && rule.daysOfWeek.contains(dayOfWeekIso)
                }
                is RecurrenceRule.Monthly -> {
                    if (rule.isLastDay) {
                        // Check if currentDate is the last day of its month
                        val nextDay = currentDate.plus(1, DateTimeUnit.DAY)
                        currentDate.month != nextDay.month
                    } else {
                        val targetDay = rule.dayOfMonth ?: startDate.dayOfMonth
                        currentDate.dayOfMonth == targetDay
                    }
                }
                is RecurrenceRule.Yearly -> {
                    currentDate.monthNumber == rule.month && currentDate.dayOfMonth == rule.dayOfMonth
                }
            }

            if (isOccurring && currentDate >= startDate) {
                // Construct instant using the original time of day
                try {
                    val occurrenceDateTime = kotlinx.datetime.LocalDateTime(
                        year = currentDate.year,
                        monthNumber = currentDate.monthNumber,
                        dayOfMonth = currentDate.dayOfMonth,
                        hour = startDateTime.hour,
                        minute = startDateTime.minute,
                        second = startDateTime.second,
                        nanosecond = startDateTime.nanosecond
                    )
                    val occurrenceMs = occurrenceDateTime.toInstant(timeZone).toEpochMilliseconds()
                    if (occurrenceMs in rangeStartMs..rangeEndMs) {
                        occurrences.add(occurrenceMs)
                    }
                } catch (e: Exception) {
                    // Ignore invalid dates like Feb 29 on a non-leap year if any logic flaws
                }
            }

            // Move to next day
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }

        return occurrences
    }
}
