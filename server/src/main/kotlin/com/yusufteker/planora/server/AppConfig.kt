package com.yusufteker.planora.server

import io.github.cdimascio.dotenv.dotenv

/**
 * Central configuration object.
 * Reads environment variables from system environment first, then from .env file via dotenv-kotlin.
 */
object AppConfig {
    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    private fun getEnv(key: String): String? {
        return System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: (try { dotenv[key] } catch (e: Exception) { null })?.takeIf { it.isNotBlank() }
    }

    // Database
    val dbUrl: String = getEnv("DB_URL") ?: ""
    val dbUser: String = getEnv("DB_USER") ?: ""
    val dbPassword: String = getEnv("DB_PASSWORD") ?: ""

    // JWT Authentication
    val jwtSecret: String = getEnv("JWT_SECRET")
        ?: error("CRITICAL SECURITY ERROR: JWT_SECRET is not configured! Define it in server/.env or system environment.")
    val jwtIssuer: String = getEnv("JWT_ISSUER") ?: "planora"

    // Gemini AI Key
    val geminiApiKey: String = getEnv("GEMINI_API_KEY")
        ?: error("CRITICAL SECURITY ERROR: GEMINI_API_KEY is not configured! Define it in server/.env or system environment.")

    // Admin Secret Key for managing users and sending admin push notifications
    val adminSecretKey: String = getEnv("ADMIN_SECRET_KEY")
        ?: error("CRITICAL SECURITY ERROR: ADMIN_SECRET_KEY is not configured! Define it in server/.env or system environment.")
}
