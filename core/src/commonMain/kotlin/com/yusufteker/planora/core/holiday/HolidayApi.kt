package com.yusufteker.planora.core.holiday

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.github.aakira.napier.Napier

/**
 * Resmi tatil verilerini API Ninjas (ve fallback olarak Nager.Date API) üzerinden çeken istemci.
 */
class HolidayApi(
    private val httpClient: HttpClient
) {
    /**
     * Belirtilen ülke ve yıl için resmi tatil listesini döner.
     * Öncelikli olarak API Ninjas API'sini dener, API Key yoksa veya başarısız olursa Nager.Date API'yi dener.
     */
    suspend fun fetchPublicHolidays(countryCode: String, year: Int, apiKey: String? = null): List<HolidayDto> {
        if (!apiKey.isNullOrBlank()) {
            try {
                Napier.d("Fetching holidays from API Ninjas for country=$countryCode year=$year", tag = "HolidayApi")
                val response: List<HolidayDto> = httpClient.get("https://api.api-ninjas.com/v1/publicholidays") {
                    header("X-Api-Key", apiKey)
                    parameter("country", countryCode)
                    parameter("year", year)
                }.body()
                if (response.isNotEmpty()) {
                    return response
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Napier.w("API Ninjas holidays request failed, falling back to Nager.Date: ${e.message}", tag = "HolidayApi")
            }
        }

        // Fallback: Nager.Date Public Holidays API (No API key required)
        return try {
            Napier.d("Fetching holidays from Nager.Date API for country=$countryCode year=$year", tag = "HolidayApi")
            val nagerResponse: List<NagerHolidayDto> = httpClient.get("https://date.nager.at/api/v3/PublicHolidays/$year/$countryCode").body()
            nagerResponse.map {
                HolidayDto(
                    name = it.localName ?: it.name ?: "Official Holiday",
                    date = it.date,
                    country = countryCode
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Napier.e("Failed to fetch holidays from fallback Nager.Date API: ${e.message}", tag = "HolidayApi")
            emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
private data class NagerHolidayDto(
    val date: String,
    val localName: String? = null,
    val name: String? = null,
    val countryCode: String? = null
)
