package com.yusufteker.planora.core.holiday

import com.yusufteker.planora.core.database.PlanoraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import io.github.aakira.napier.Napier

/**
 * Resmi tatil verilerini yöneten repository.
 * Offline-First mimarisine uygundur: Önce yerel SQLDelight veritabanını kontrol eder,
 * veriler yoksa API'den çekip yerele kaydeder.
 */
class HolidayRepository(
    private val holidayApi: HolidayApi,
    private val database: PlanoraDatabase
) {
    /**
     * Ülke ve yıla ait resmi tatilleri getirir.
     * @param countryCode ISO-2 Ülke kodu (örn: "TR", "US")
     * @param year Yıl (örn: 2026)
     * @param apiKey Opsiyonel API Ninjas anahtarı
     */
    suspend fun getHolidays(countryCode: String, year: Int, apiKey: String? = com.yusufteker.planora.core.config.AppConfig.apiNinjasKey): Map<String, String> = withContext(Dispatchers.IO) {
        val upperCountry = countryCode.uppercase()
        val queries = database.planoraDatabaseQueries
        
        try {
            queries.createHolidayTableIfNotExists()
        } catch (e: Exception) {
            Napier.w("Could not execute createHolidayTableIfNotExists: ${e.message}", tag = "HolidayRepository")
        }

        // 1. Önce yerel SQLDelight veritabanından sorgula
        val cached = queries.getHolidaysByCountryAndYear(upperCountry, year.toLong()).executeAsList()
        if (cached.isNotEmpty()) {
            Napier.d("Returning ${cached.size} holidays from local DB for country=$upperCountry year=$year", tag = "HolidayRepository")
            return@withContext cached.associate { it.date to it.name }
        }

        // 2. Yerelde yoksa API'den çek
        Napier.d("Fetching holidays from API for country=$upperCountry year=$year", tag = "HolidayRepository")
        val remoteHolidays = holidayApi.fetchPublicHolidays(upperCountry, year, apiKey)

        if (remoteHolidays.isNotEmpty()) {
            // 3. Çekilen verileri SQLDelight veritabanına kaydet
            queries.transaction {
                remoteHolidays.forEach { item ->
                    queries.insertHoliday(
                        date = item.date,
                        name = item.name,
                        countryCode = upperCountry,
                        year = year.toLong()
                    )
                }
            }
            Napier.d("Saved ${remoteHolidays.size} holidays to local DB", tag = "HolidayRepository")
            return@withContext remoteHolidays.associate { it.date to it.name }
        }

        emptyMap()
    }
}
