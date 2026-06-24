package com.yusufteker.pulse.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import com.yusufteker.pulse.server.database.DatabaseFactory
import com.yusufteker.pulse.server.plugins.configureSecurity
import com.yusufteker.pulse.server.routes.authRoutes

/**
 * Main entry point for the Ktor server.
 */
fun main(args: Array<String>) {
    // Load local .env file manually into System properties so application.conf can resolve them.
    val envFile = File(".env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    System.setProperty(parts[0].trim(), parts[1].trim())
                }
            }
        }
    }

    // Start Ktor using Netty engine and application.conf
    EngineMain.main(args)
}

/**
 * Ktor Module entry point referenced in application.conf
 */
fun Application.module() {
    // 1. Veritabanını, Hikari havuzunu ve Flyway Migration'ı başlat.
    DatabaseFactory.init(environment.config)

    // 2. JSON veri alıp göndermek için ContentNegotiation eklentisini kur.
    install(ContentNegotiation) {
        json()
    }

    // 3. Gelen JWT tokenlarını doğrulamak için yazdığımız Security plugin'i kur.
    configureSecurity()

    // 4. API rotalarını yönlendir.
    routing {
        authRoutes()
    }
}
