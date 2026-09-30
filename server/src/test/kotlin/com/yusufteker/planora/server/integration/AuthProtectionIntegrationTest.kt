package com.yusufteker.planora.server.integration

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.yusufteker.planora.server.AppConfig
import com.yusufteker.planora.server.plugins.configureSecurity
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests verifying JWT Authentication & Authorization filters.
 *
 * Verifies that protected routes protected by the "auth-jwt" provider:
 * - Reject unauthenticated requests with 401 Unauthorized
 * - Reject tokens signed with incorrect secrets or missing claims with 401 Unauthorized
 * - Accept valid tokens containing expected claims and allow handler execution
 */
class AuthProtectionIntegrationTest {

    /**
     * Helper creating a test signed JWT token matching the server's expected configuration.
     */
    private fun createTestToken(userId: Int = 1, audience: String = "planora-client", secret: String = AppConfig.jwtSecret): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(AppConfig.jwtIssuer)
            .withClaim("userId", userId)
            .withClaim("email", "testuser@example.com")
            .sign(Algorithm.HMAC256(secret))
    }

    /**
     * Verifies that requests without an Authorization header to a protected route return 401 Unauthorized.
     */
    @Test
    fun protectedRouteWithoutTokenShouldReturn401Unauthorized() = testApplication {
        application {
            configureSecurity()
            routing {
                authenticate("auth-jwt") {
                    get("/api/protected-data") {
                        call.respondText("Confidential")
                    }
                }
            }
        }

        val response = client.get("/api/protected-data")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Verifies that a token signed with a forged/wrong secret returns 401 Unauthorized.
     */
    @Test
    fun protectedRouteWithForgedTokenShouldReturn401Unauthorized() = testApplication {
        application {
            configureSecurity()
            routing {
                authenticate("auth-jwt") {
                    get("/api/protected-data") {
                        call.respondText("Confidential")
                    }
                }
            }
        }

        val forgedToken = createTestToken(secret = "completely_different_hacker_secret_key")
        val response = client.get("/api/protected-data") {
            header("Authorization", "Bearer $forgedToken")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Verifies that a valid signed JWT token grants access to protected routes and supplies the principal.
     */
    @Test
    fun protectedRouteWithValidTokenShouldReturn200AndExtractUser() = testApplication {
        application {
            configureSecurity()
            routing {
                authenticate("auth-jwt") {
                    get("/api/protected-data") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.getClaim("userId")?.asInt()
                        call.respondText("Hello user $userId")
                    }
                }
            }
        }

        val validToken = createTestToken(userId = 42)
        val response = client.get("/api/protected-data") {
            header("Authorization", "Bearer $validToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello user 42", response.bodyAsText())
    }
}
