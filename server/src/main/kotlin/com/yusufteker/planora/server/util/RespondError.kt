package com.yusufteker.planora.server.util

import com.yusufteker.planora.shared.api.ApiErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * Responds to the client with a standardized [ApiErrorResponse] JSON object and specified [status].
 *
 * @param status HTTP status code (e.g. [HttpStatusCode.Conflict], [HttpStatusCode.Unauthorized])
 * @param code Standard error code string from [com.yusufteker.planora.shared.api.ApiErrorCode]
 * @param message Optional English developer/debug message
 */
suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String? = null
) {
    respond(status, ApiErrorResponse(code = code, message = message))
}
