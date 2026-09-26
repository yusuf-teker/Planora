package com.yusufteker.planora.server.util

import com.yusufteker.planora.shared.api.ApiErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond

/**
 * Responds to the client with a standardized [ApiErrorResponse] JSON object and specified [status].
 * Automatically attaches the current request's unique [callId] (Request ID / Correlation ID) for traceability.
 *
 * @param status HTTP status code (e.g. [HttpStatusCode.Conflict], [HttpStatusCode.Unauthorized], [HttpStatusCode.BadRequest])
 * @param code Standard error code string from [com.yusufteker.planora.shared.api.ApiErrorCode]
 * @param message Optional English developer/debug message
 * @param fieldErrors Optional map of invalid field names to error messages (RFC 7807 problem details)
 * @param requestId Optional request ID override (defaults to current [ApplicationCall.callId])
 */
suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String? = null,
    fieldErrors: Map<String, String>? = null,
    requestId: String? = this.callId
) {
    respond(status, ApiErrorResponse(code = code, message = message, fieldErrors = fieldErrors, requestId = requestId))
}
