package com.yusufteker.planora.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import java.util.UUID

/**
 * Standard HTTP header for unique request identification.
 */
const val HEADER_REQUEST_ID = "X-Request-ID"

/**
 * Standard HTTP header for distributed multi-request correlation.
 */
const val HEADER_CORRELATION_ID = "X-Correlation-ID"

/**
 * Configures the Ktor [CallId] plugin to track and correlate all incoming HTTP requests.
 *
 * How it works:
 * 1. Checks if the client passed [HEADER_REQUEST_ID] or [HEADER_CORRELATION_ID].
 * 2. If present and non-blank, reuses that ID for tracing.
 * 3. If missing, automatically generates a new UUID.
 * 4. Automatically attaches the resolved ID to the HTTP response headers via [HEADER_REQUEST_ID].
 */
fun Application.configureCallId() {
    install(CallId) {
        header(HEADER_REQUEST_ID)
        header(HEADER_CORRELATION_ID)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
        replyToHeader(HEADER_REQUEST_ID)
    }
}
