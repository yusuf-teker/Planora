package com.yusufteker.planora.server.plugins

import com.yusufteker.planora.server.util.respondError
import com.yusufteker.planora.shared.api.ApiErrorCode
import com.yusufteker.planora.shared.api.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import kotlinx.serialization.SerializationException

/**
 * Configures the Ktor [StatusPages] plugin.
 *
 * Intercepts uncaught exceptions and input validation errors, converting them
 * into standardized [com.yusufteker.planora.shared.api.ApiErrorResponse] JSON objects.
 * This guarantees consistent error formatting (inspired by RFC 7807) across all endpoints
 * and prevents stack trace leakage to clients.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        // 1. Fail-Fast input validation errors
        exception<ValidationException> { call, cause ->
            call.respondError(
                status = HttpStatusCode.BadRequest,
                code = ApiErrorCode.VALIDATION_ERROR,
                message = cause.message,
                fieldErrors = cause.fieldErrors
            )
        }

        // 2. Malformed JSON or mismatched/missing required serialization properties
        exception<SerializationException> { call, cause ->
            call.respondError(
                status = HttpStatusCode.BadRequest,
                code = ApiErrorCode.BAD_REQUEST,
                message = "Malformed JSON payload or invalid property types: ${cause.message?.substringBefore('\n')}"
            )
        }

        // 3. Ktor standard BadRequestException (e.g. invalid query parameters or content conversion)
        exception<BadRequestException> { call, cause ->
            call.respondError(
                status = HttpStatusCode.BadRequest,
                code = ApiErrorCode.BAD_REQUEST,
                message = cause.message ?: "Bad request syntax or parameter"
            )
        }

        // 4. Rate Limit (429 Too Many Requests)
        status(HttpStatusCode.TooManyRequests) { call, _ ->
            val retryAfter = call.response.headers["Retry-After"]
            val hint = if (retryAfter != null) " Please retry after $retryAfter seconds." else " Please try again later."
            call.respondError(
                status = HttpStatusCode.TooManyRequests,
                code = ApiErrorCode.TOO_MANY_REQUESTS,
                message = "Too many requests.$hint"
            )
        }

        // 5. Catch-all for unexpected runtime exceptions
        exception<Throwable> { call, cause ->
            val reqId = call.callId ?: "-"
            call.application.log.error("Unhandled server exception [$reqId] encountered on ${call.request.uri}:", cause)
            call.respondError(
                status = HttpStatusCode.InternalServerError,
                code = ApiErrorCode.INTERNAL_SERVER_ERROR,
                message = "An unexpected internal server error occurred"
            )
        }
    }
}
