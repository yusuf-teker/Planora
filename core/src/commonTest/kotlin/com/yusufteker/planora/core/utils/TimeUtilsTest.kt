package com.yusufteker.planora.core.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pulse uygulamasında Görev (Task) ve Etkinlik (Event) kopyalama (Duplicate/Copy)
 * sırasında tarihlerin doğruluğunu test eden birim testi.
 *
 * İş Kuralı:
 * Kullanıcı geçmiş veya gelecek bir görevi/etkinliği kopyaladığında:
 * 1. Tarih BUGÜNE (Today) çekilmelidir.
 * 2. Orijinal etkinliğin SAATİ ve DAKİKASI birebir korunmalıdır.
 * 3. Etkinliğin SÜRESİ (Duration: Bitiş - Başlangıç) bozulmadan bugüne aktarılmalıdır.
 */
class TimeUtilsTest {

    private val tz = TimeZone.currentSystemDefault()

    @Test
    fun `gecmisteki gorev kopyalandiginda gun bugune gelmeli ancak saat ve dakika korunmalidir`() {
        // 1. Arrange (Hazırlık): Geçmiş bir tarih oluştur (Örn: 15 Ocak 2024, Saat 14:35)
        val pastOriginalDateTime = LocalDateTime(
            year = 2024,
            month = kotlinx.datetime.Month.JANUARY,
            day = 15,
            hour = 14,
            minute = 35
        )
        val pastOriginalMs = pastOriginalDateTime.toInstant(tz).toEpochMilliseconds()

        val nowMs = getCurrentTimeMs()
        val todayExpectedDate = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date

        // 2. Act (Eylem): Pulse'ın kopyalama fonksiyonunu çağır
        val copiedTaskTimeMs = getTodayWithOriginalTime(pastOriginalMs)
        val copiedDateTime = Instant.fromEpochMilliseconds(copiedTaskTimeMs).toLocalDateTime(tz)

        // 3. Assert (Doğrulama):
        // Gün, ay ve yıl BUGÜN ile aynı olmalı!
        assertEquals(todayExpectedDate.year, copiedDateTime.year, "Kopyalanan görevin yılı bugünün yılı olmalıdır")
        assertEquals(todayExpectedDate.month, copiedDateTime.month, "Kopyalanan görevin ayı bugünün ayı olmalıdır")
        assertEquals(todayExpectedDate.day, copiedDateTime.day, "Kopyalanan görevin günü bugünün günü olmalıdır")

        // Ancak saat ve dakika orijinal görevden (14:35) korunmalıdır!
        assertEquals(14, copiedDateTime.hour, "Orijinal saat (14) korunmalıdır")
        assertEquals(35, copiedDateTime.minute, "Orijinal dakika (35) korunmalıdır")
        assertEquals(0, copiedDateTime.second, "Saniyeler 0 olarak normalize edilmelidir")
    }

    @Test
    fun `etkinlik kopyalandiginda sure duration bozulmadan bugune tasinmalidir`() {
        // 1. Arrange: Orijinal etkinlik: Geçmişte 10:00 ile 11:30 arası (90 dakika süreli)
        val originalStart = LocalDateTime(2024, kotlinx.datetime.Month.MAY, 10, 10, 0).toInstant(tz).toEpochMilliseconds()
        val originalEnd = LocalDateTime(2024, kotlinx.datetime.Month.MAY, 10, 11, 30).toInstant(tz).toEpochMilliseconds()
        val originalDurationMs = originalEnd - originalStart // 90 dakika = 5.400.000 ms

        // 2. Act: EventEditorViewModel kopyalama mantığını simüle et:
        val todayStartMs = getTodayWithOriginalTime(originalStart)
        val todayEndMs = todayStartMs + originalDurationMs

        val todayStartLocal = Instant.fromEpochMilliseconds(todayStartMs).toLocalDateTime(tz)
        val todayEndLocal = Instant.fromEpochMilliseconds(todayEndMs).toLocalDateTime(tz)

        // 3. Assert:
        // Süre 90 dakika olarak korunmuş mu?
        assertEquals(originalDurationMs, todayEndMs - todayStartMs, "Etkinliğin süresi kopyalandıktan sonra da 90 dakika olmalıdır")

        // Başlangıç saati 10:00, bitiş saati 11:30 mu?
        assertEquals(10, todayStartLocal.hour)
        assertEquals(0, todayStartLocal.minute)
        assertEquals(11, todayEndLocal.hour)
        assertEquals(30, todayEndLocal.minute)
    }

    @Test
    fun `orijinal zaman null verildiginde anlik zamana yakin bir deger donmelidir`() {
        // Arrange
        val beforeMs = getCurrentTimeMs()

        // Act
        val resultMs = getTodayWithOriginalTime(null)

        val afterMs = getCurrentTimeMs()

        // Assert: Dönen zaman bu testin çalıştığı an ile örtüşmeli
        assertTrue(resultMs in beforeMs..afterMs, "Orijinal zaman null ise şu anki zaman dönmelidir")
    }
}
