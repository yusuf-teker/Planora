package com.yusufteker.planora.core.utils

import com.yusufteker.planora.shared.api.RecurrenceRule
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pulse uygulamasının tekrarlayan görev motorunu (RecurringTaskEvaluator)
 * test eden gerçek iş mantığı birim testi.
 */
class RecurringTaskEvaluatorTest {

    private val timeZone = TimeZone.UTC

    @Test
    fun `baslangic zamani aralik bitisinden buyukse bos liste donmelidir`() {
        // Arrange (Hazırlık)
        val startTime = LocalDate(2026, 9, 20).atStartOfDayIn(timeZone).toEpochMilliseconds()
        val rangeStart = LocalDate(2026, 9, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
        val rangeEnd = LocalDate(2026, 9, 10).atStartOfDayIn(timeZone).toEpochMilliseconds()

        // Act (Eylem)
        val result = RecurringTaskEvaluator.generateOccurrences(
            startTimeMs = startTime,
            rule = RecurrenceRule.Daily(interval = 1),
            rangeStartMs = rangeStart,
            rangeEndMs = rangeEnd,
            timeZone = timeZone
        )

        // Assert (Doğrulama)
        assertTrue(result.isEmpty(), "Aralığın dışındaki başlangıç için liste boş olmalıdır")
    }

    @Test
    fun `gunluk tekrar 1 gun aralikla secildiginde 5 gunluk aralikta 5 tekrar uretmelidir`() {
        // Arrange: 1 Eylül ile 5 Eylül arasında günlük tekrar (1, 2, 3, 4, 5 Eylül)
        val startDate = LocalDate(2026, 9, 1)
        val endDate = LocalDate(2026, 9, 5)

        val startTimeMs = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val rangeEndMs = endDate.atStartOfDayIn(timeZone).toEpochMilliseconds()

        // Act
        val occurrences = RecurringTaskEvaluator.generateOccurrences(
            startTimeMs = startTimeMs,
            rule = RecurrenceRule.Daily(interval = 1),
            rangeStartMs = startTimeMs,
            rangeEndMs = rangeEndMs,
            timeZone = timeZone
        )

        // Assert
        assertEquals(5, occurrences.size, "1-5 Eylül arasında 5 adet günlük tekrar üretilmelidir")
    }

    @Test
    fun `2 gunde bir tekrar secildiginde gunleri atlayarak uretmelidir`() {
        // Arrange: 1 Eylül'den 5 Eylül'e kadar (1, 3, 5 Eylül olmak üzere 3 adet)
        val startDate = LocalDate(2026, 9, 1)
        val endDate = LocalDate(2026, 9, 5)

        val startTimeMs = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val rangeEndMs = endDate.atStartOfDayIn(timeZone).toEpochMilliseconds()

        // Act
        val occurrences = RecurringTaskEvaluator.generateOccurrences(
            startTimeMs = startTimeMs,
            rule = RecurrenceRule.Daily(interval = 2),
            rangeStartMs = startTimeMs,
            rangeEndMs = rangeEndMs,
            timeZone = timeZone
        )

        // Assert
        assertEquals(3, occurrences.size, "1, 3 ve 5 Eylül için 3 tekrar üretilmelidir")
    }
}
