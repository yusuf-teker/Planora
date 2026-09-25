package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.AppConfig
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Admin rotalarının güvenlik ve yetkilendirme doğrulamalarını test eder.
 *
 * `X-Admin-Secret` başlığı bulunmadığında, boş olduğunda veya geçersiz bir anahtar
 * içerdiğinde tüm admin uç noktalarının (Push bildirimi, Premium atama vb.)
 * [HttpStatusCode.Forbidden] (403) döndürerek erişimi engellediğini doğrular.
 */
class AdminSecurityRoutesTest {

    /**
     * `X-Admin-Secret` başlığı olmadan `POST /admin/push` çağrıldığında
     * sunucunun isteği engelleyip 403 Forbidden döndüğünü doğrular.
     */
    @Test
    fun adminPushWithoutSecretHeaderShouldReturnForbidden() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { adminRoutes() }
        }

        val response = client.post("/admin/push") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Test","body":"Hello"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    /**
     * Yanlış / sahte bir `X-Admin-Secret` başlığı ile `POST /admin/push` çağrıldığında
     * sunucunun 403 Forbidden döndüğünü doğrular.
     */
    @Test
    fun adminPushWithInvalidSecretHeaderShouldReturnForbidden() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { adminRoutes() }
        }

        val response = client.post("/admin/push") {
            header("X-Admin-Secret", "hacker_invalid_secret_key_123")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Test","body":"Hello"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    /**
     * Boş string değerli bir `X-Admin-Secret` başlığı ile `POST /admin/push` çağrıldığında
     * sunucunun 403 Forbidden döndüğünü doğrular.
     */
    @Test
    fun adminPushWithEmptySecretHeaderShouldReturnForbidden() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { adminRoutes() }
        }

        val response = client.post("/admin/push") {
            header("X-Admin-Secret", "")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Test","body":"Hello"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    /**
     * `X-Admin-Secret` başlığı olmadan `POST /admin/users/set-premium` çağrıldığında
     * sunucunun 403 Forbidden döndüğünü doğrular.
     */
    @Test
    fun adminSetPremiumWithoutSecretHeaderShouldReturnForbidden() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { adminRoutes() }
        }

        val response = client.post("/admin/users/set-premium") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"victim@test.com","isPremium":true}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    /**
     * Yanlış `X-Admin-Secret` ile `POST /admin/users/set-premium` çağrıldığında
     * sunucunun 403 Forbidden döndüğünü doğrular.
     */
    @Test
    fun adminSetPremiumWithInvalidSecretHeaderShouldReturnForbidden() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { adminRoutes() }
        }

        val response = client.post("/admin/users/set-premium") {
            header("X-Admin-Secret", "tampered_key_999")
            contentType(ContentType.Application.Json)
            setBody("""{"email":"victim@test.com","isPremium":true}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    /**
     * Doğru `X-Admin-Secret` başlığı verildiğinde güvenlik kontrolünün başarıyla aşıldığını
     * (403 Forbidden dönmediğini) doğrular.
     */
    @Test
    fun adminPushWithValidSecretHeaderPassesSecurityGuard() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { adminRoutes() }
        }

        // Gövdesiz/boş çağrı yapıyoruz; güvenlik aşılırsa 400 Bad Request dönecektir, asla 403 dönmemelidir.
        val response = client.post("/admin/push") {
            header("X-Admin-Secret", AppConfig.adminSecretKey)
            contentType(ContentType.Application.Json)
            setBody("""{}""")
        }

        assertNotEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
