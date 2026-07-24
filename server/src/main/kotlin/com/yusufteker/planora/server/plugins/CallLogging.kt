package com.yusufteker.planora.server.plugins

import io.ktor.server.application.*
import io.ktor.server.request.*

fun Application.configureCallLogging() {
    intercept(ApplicationCallPipeline.Monitoring) {
        val method = call.request.httpMethod.value
        val path = call.request.uri
        proceed()
        val status = call.response.status()?.value ?: 0
        val color = when (status) {
            in 200..299 -> "\u001B[32m" // Green
            in 300..399 -> "\u001B[33m" // Yellow
            in 400..499 -> "\u001B[31m" // Red
            in 500..599 -> "\u001B[35m" // Purple
            else -> "\u001B[0m" // Reset
        }
        val reset = "\u001B[0m"
        println("[API-LOG] $color$method $path -> $status$reset")
    }
}
