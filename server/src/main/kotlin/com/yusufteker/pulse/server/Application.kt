package com.yusufteker.pulse.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import com.yusufteker.pulse.server.database.DatabaseFactory
import com.yusufteker.pulse.server.plugins.configureSecurity
import com.yusufteker.pulse.server.routes.authRoutes
import com.yusufteker.pulse.server.routes.postRoutes
import com.yusufteker.pulse.server.routes.commentRoutes
import com.yusufteker.pulse.server.routes.userRoutes
import com.yusufteker.pulse.server.routes.planRoomRoutes

/**
 * Main entry point for the Ktor server.
 */
fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

/**
 * Ktor Module entry point.
 */
fun Application.module() {
    // 1. Veritabanını, Hikari havuzunu ve Flyway Migration'ı başlat.
    DatabaseFactory.init()

    // 2. JSON veri alıp göndermek için ContentNegotiation eklentisini kur.
    install(ContentNegotiation) {
        json()
    }

    // 3. Gelen JWT tokenlarını doğrulamak için yazdığımız Security plugin'i kur.
    configureSecurity()

    // 4. API rotalarını yönlendir.
    routing {
        authRoutes()
        postRoutes()
        commentRoutes()
        userRoutes()
        planRoomRoutes()
    }
}
