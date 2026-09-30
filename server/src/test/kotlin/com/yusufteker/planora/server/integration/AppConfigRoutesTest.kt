package com.yusufteker.planora.server.integration

import com.yusufteker.planora.server.routes.appConfigRoutes
import com.yusufteker.planora.shared.api.AppVersionConfigDto
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the App Config and Remote Versioning endpoint.
 *
 * Validates that the client application can query minimum supported and latest
 * versions, localized update prompts, and direct store URLs.
 */
class AppConfigRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Verifies that `GET /api/app-config` responds with 200 OK and valid version metadata.
     */
    @Test
    fun getAppConfigShouldReturn200AndValidVersionMetadata() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { appConfigRoutes() }
        }

        val response = client.get("/api/app-config")
        assertEquals(HttpStatusCode.OK, response.status)

        val config = json.decodeFromString<AppVersionConfigDto>(response.bodyAsText())
        assertNotNull(config)
        assertTrue(config.minSupportedVersion.isNotBlank(), "Min supported version should not be blank")
        assertTrue(config.latestVersion.isNotBlank(), "Latest version should not be blank")
        assertEquals("Zorunlu Güncelleme", config.forceUpdateTitleTr)
        assertEquals("Update Required", config.forceUpdateTitleEn)
    }
}
