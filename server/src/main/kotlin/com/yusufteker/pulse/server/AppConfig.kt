package com.yusufteker.pulse.server

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
    val jwtIssuer: String = dotenv["JWT_ISSUER", "pulse"]
}
