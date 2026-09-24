package com.yusufteker.planora.server

import io.github.cdimascio.dotenv.dotenv

/**
 * Central configuration object.
 * Reads all values from .env file via dotenv-kotlin.
 */
object AppConfig {
    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    // Database
    val dbUrl: String = dotenv["DB_URL"]
    val dbUser: String = dotenv["DB_USER"]
    val dbPassword: String = dotenv["DB_PASSWORD"]

    // JWT
    val jwtSecret: String = dotenv["JWT_SECRET", "secret"]
    val jwtIssuer: String = dotenv["JWT_ISSUER", "planora"]

    // Gemini AI Key
    val geminiApiKey: String = System.getenv("GEMINI_API_KEY")
        ?.takeIf { it.isNotBlank() }
        ?: (try { dotenv["GEMINI_API_KEY"] } catch (e: Exception) { null })
        ?.takeIf { it.isNotBlank() }
        ?: "AQ.Ab8RN6LpQCJ68aFDG2th5ewgmLgi1cTNmHf8j62LAD3gPYeHUQ"

    // Admin Secret Key for managing users
    val adminSecretKey: String = System.getenv("ADMIN_SECRET_KEY") ?: (try { dotenv["ADMIN_SECRET_KEY"] } catch (e: Exception) { "planora_admin_secret_2026" })
}
