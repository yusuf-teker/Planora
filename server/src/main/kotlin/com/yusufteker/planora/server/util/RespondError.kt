package com.yusufteker.planora.server.util

import com.yusufteker.planora.shared.api.ApiErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * Responds to the client with a standardized [ApiErrorResponse] JSON object and specified [status].
 *
 * @param status HTTP status code (e.g. [HttpStatusCode.Conflict], [HttpStatusCode.Unauthorized], [HttpStatusCode.BadRequest])
 * @param code Standard error code string from [com.yusufteker.planora.shared.api.ApiErrorCode]
 * @param message Optional English developer/debug message
 * @param fieldErrors Optional map of invalid field names to error messages (RFC 7807 problem details)
 */
suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String? = null,
    fieldErrors: Map<String, String>? = null
) {
    respond(status, ApiErrorResponse(code = code, message = message, fieldErrors = fieldErrors))
}
