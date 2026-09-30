package com.yusufteker.planora.server.integration

import com.yusufteker.planora.server.plugins.configureStatusPages
import com.yusufteker.planora.shared.api.ApiErrorCode
import com.yusufteker.planora.shared.api.ApiErrorResponse
import com.yusufteker.planora.shared.api.ValidationException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Serializable
private data class SampleRequestBody(
    val title: String,
    val count: Int
)

/**
 * Integration tests verifying RFC 7807 error handling and format standardization via StatusPages.
 *
 * Verifies that validation errors, malformed JSON payloads, and bad requests
 * are converted into consistent [ApiErrorResponse] structures without leaking stack traces.
 */
class StatusPagesIntegrationTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Verifies that throwing a [ValidationException] in a route returns 400 Bad Request
     * with [ApiErrorCode.VALIDATION_ERROR] and field-level error mapping.
     */
    @Test
    fun validationExceptionShouldReturn400WithFieldErrors() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            configureStatusPages()
            routing {
                get("/test/validation-error") {
                    throw ValidationException(mapOf("email" to "Invalid email format"))
                }
            }
        }

        val response = client.get("/test/validation-error")
        assertEquals(HttpStatusCode.BadRequest, response.status)

        val errorResponse = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
        assertEquals(ApiErrorCode.VALIDATION_ERROR, errorResponse.code)
        assertNotNull(errorResponse.fieldErrors)
        assertEquals("Invalid email format", errorResponse.fieldErrors?.get("email"))
    }

    /**
     * Verifies that sending a malformed or broken JSON body returns 400 Bad Request
     * with [ApiErrorCode.BAD_REQUEST] rather than an unhandled 500 internal server error.
     */
    @Test
    fun malformedJsonPayloadShouldReturn400BadRequest() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            configureStatusPages()
            routing {
                post("/test/receive-json") {
                    val body = call.receive<SampleRequestBody>()
                    call.respond(HttpStatusCode.OK, body)
                }
            }
        }

        // Broken JSON without closing braces or wrong types
        val response = client.post("/test/receive-json") {
            contentType(ContentType.Application.Json)
            setBody("""{ "title": "Incomplete JSON", "count": "not_a_number" """)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorResponse = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
        assertEquals(ApiErrorCode.BAD_REQUEST, errorResponse.code)
    }
}
