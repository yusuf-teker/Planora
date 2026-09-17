package com.yusufteker.planora.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import com.yusufteker.planora.server.database.DatabaseFactory
import com.yusufteker.planora.server.plugins.configureSecurity
import com.yusufteker.planora.server.plugins.configureCallLogging
import com.yusufteker.planora.server.routes.authRoutes
import com.yusufteker.planora.server.routes.postRoutes
import com.yusufteker.planora.server.routes.commentRoutes
import com.yusufteker.planora.server.routes.userRoutes
import com.yusufteker.planora.server.routes.planRoomRoutes
import com.yusufteker.planora.server.routes.taskRoutes
import com.yusufteker.planora.server.routes.calendarRoutes

import com.yusufteker.planora.server.routes.appConfigRoutes
import com.yusufteker.planora.server.routes.fcmTokenRoutes
import com.yusufteker.planora.server.service.FcmService

/**
 * Main entry point for the Ktor server.
 */
fun main() {
    // Render dinamik olarak PORT atar. Bulamazsa 8080 kullanır.
    val port = System.getenv("PORT")?.toInt() ?: 8080
    // Render'da dışarıdan erişilebilmesi için host "0.0.0.0" olmalıdır.
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
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

    // 4. Çağrı loglarını yapılandır (Renkli loglar)
    configureCallLogging()

    // Firebase Init
    FcmService.init()

    // 5. API rotalarını yönlendir.
    routing {
        get("/") {
            call.respondText("Planora Server is Running!", status = HttpStatusCode.OK)
        }
        head("/") {
            call.respond(HttpStatusCode.OK)
        }
        // Lightweight ping endpoint for keep-alive and uptime monitoring
        get("/ping") {
            call.respondText("pong", status = HttpStatusCode.OK)
        }
        head("/ping") {
            call.respond(HttpStatusCode.OK)
        }
        // Health check endpoint
        get("/health") {
            call.respondText("OK", status = HttpStatusCode.OK)
        }
        head("/health") {
            call.respond(HttpStatusCode.OK)
        }
        appConfigRoutes()
        authRoutes()
        postRoutes()
        commentRoutes()
        userRoutes()
        planRoomRoutes()
        taskRoutes()
        fcmTokenRoutes()
        calendarRoutes()
    }
}
