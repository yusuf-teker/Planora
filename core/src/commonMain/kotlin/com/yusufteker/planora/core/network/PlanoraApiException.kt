package com.yusufteker.planora.core.network

import com.yusufteker.planora.shared.api.ApiErrorResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException

/**
 * Exception thrown when the backend returns a structured [ApiErrorResponse].
 *
 * @param errorResponse The parsed error response from the server containing an error code.
 * @param statusCode The HTTP status code (e.g. 400, 401, 404, 409, 500).
 */
class PlanoraApiException(
    val errorResponse: ApiErrorResponse,
    val statusCode: Int
) : Exception(errorResponse.message ?: "API error code: ${errorResponse.code} (HTTP $statusCode)") {
    /**
     * Map of specific input field names to validation failure reasons (inspired by RFC 7807),
     * populated when [errorResponse] code is [com.yusufteker.planora.shared.api.ApiErrorCode.VALIDATION_ERROR].
     */
    val fieldErrors: Map<String, String>? get() = errorResponse.fieldErrors

    /**
     * Unique Request ID / Correlation ID echoed from the server, useful for user error reporting and log tracing.
     */
    val requestId: String? get() = errorResponse.requestId
}

/**
 * Extension function that inspects a [Throwable] (e.g. Ktor [ResponseException])
 * and parses its JSON body into a [PlanoraApiException] if available.
 */
suspend fun Throwable.parseApiException(): Throwable {
    if (this is ResponseException) {
        return try {
            val errorResponse = response.body<ApiErrorResponse>()
            PlanoraApiException(errorResponse, response.status.value)
        } catch (e: Exception) {
            this
        }
    }
    return this
}
