package com.yusufteker.planora.core.data.api

import com.yusufteker.planora.core.data.dto.AppVersionConfigDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Remote API service for fetching application version and update configuration.
 *
 * @property httpClient Configured [HttpClient] for HTTP execution.
 */
class AppVersionApi(private val httpClient: HttpClient) {

    /**
     * Fetches the latest version configuration from the server endpoint `api/app-config`.
     *
     * @return [Result] containing [AppVersionConfigDto] on success, or exception on failure.
     */
    suspend fun fetchAppVersionConfig(): Result<AppVersionConfigDto> {
        return try {
            val response: AppVersionConfigDto = httpClient.get("api/app-config").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
