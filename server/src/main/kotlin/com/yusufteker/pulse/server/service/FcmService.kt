package com.yusufteker.pulse.server.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.FcmTokenEntity
import com.yusufteker.pulse.server.database.tables.FcmTokensTable
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

object FcmService {
    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    fun init() {
        try {
            val serviceAccount = File("firebase-service-account.json")
            if (serviceAccount.exists()) {
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(FileInputStream(serviceAccount)))
                    .build()

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options)
                    logger.info("Firebase Admin initialized successfully.")
                }
            } else {
                logger.warn("firebase-service-account.json not found. Push notifications will be disabled.")
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase Admin: ${e.message}", e)
        }
    }

    suspend fun sendPushToUser(userId: Int, title: String, body: String) {
        val tokens = dbQuery {
            FcmTokenEntity.find { FcmTokensTable.userId eq userId }.map { it.token }
        }

        if (tokens.isEmpty()) return

        tokens.forEach { token ->
            try {
                val message = Message.builder()
                    .setToken(token)
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build()
                    )
                    .putData("title", title)
                    .putData("body", body)
                    .build()

                val response = FirebaseMessaging.getInstance().send(message)
                logger.info("Successfully sent message: $response")
            } catch (e: Exception) {
                logger.error("Error sending push notification to user $userId: ${e.message}", e)
                // TODO: Handle token cleanup if token is invalid
            }
        }
    }
}
