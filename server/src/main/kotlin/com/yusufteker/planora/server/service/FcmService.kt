package com.yusufteker.planora.server.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.FcmTokenEntity
import com.yusufteker.planora.server.database.tables.FcmTokensTable
import com.yusufteker.planora.server.database.tables.PlanRoomMembersTable
import com.yusufteker.planora.shared.api.RoomMemberStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Base64

object FcmService {
    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    fun init() {
        try {
            val result = loadCredentials()
            if (result != null) {
                val (credentials, sourceInfo) = result
                val options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build()

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options)
                    logger.info("Firebase Admin initialized successfully from $sourceInfo.")
                }
            } else {
                logger.warn("Firebase service account credentials not found or invalid across all sources (ENV Base64/JSON/PATH, Render secrets /etc/secrets/firebase-service-account.json, or local root/server). Push notifications will be disabled.")
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase Admin: ${e.message}", e)
        }
    }

    private fun loadCredentials(): Pair<GoogleCredentials, String>? {
        // 1. Environment variable: Base64 string
        val base64Env = System.getenv("FIREBASE_SERVICE_ACCOUNT_BASE64")
            ?: System.getenv("FIREBASE_CREDENTIALS_BASE64")
        if (!base64Env.isNullOrBlank()) {
            val creds = tryDecodeBase64Credentials(base64Env)
            if (creds != null) {
                return creds to "FIREBASE_SERVICE_ACCOUNT_BASE64 environment variable"
            } else {
                logger.warn("Could not decode valid credentials from FIREBASE_SERVICE_ACCOUNT_BASE64. Trying next fallbacks...")
            }
        }

        // 2. Environment variable: Raw JSON string
        val rawJsonEnv = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON")
            ?: System.getenv("FIREBASE_CREDENTIALS_JSON")
        if (!rawJsonEnv.isNullOrBlank()) {
            val creds = tryLoadJsonCredentials(rawJsonEnv)
            if (creds != null) {
                return creds to "FIREBASE_SERVICE_ACCOUNT_JSON environment variable"
            } else {
                logger.warn("Could not decode valid credentials from FIREBASE_SERVICE_ACCOUNT_JSON. Trying next fallbacks...")
            }
        }

        // 3. Environment variable: File path
        val filePathEnv = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
            ?: System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        if (!filePathEnv.isNullOrBlank()) {
            val file = File(filePathEnv)
            if (file.exists()) {
                val creds = tryLoadFileCredentials(file)
                if (creds != null) {
                    return creds to "file at ${file.absolutePath} (from env)"
                }
            }
        }

        // 4. Fallback: Render Secret Files or Local files
        val possibleFiles = listOf(
            File("/etc/secrets/firebase-service-account.json"),
            File("firebase-service-account.json"),
            File("server/firebase-service-account.json")
        )
        for (file in possibleFiles) {
            if (file.exists()) {
                val creds = tryLoadFileCredentials(file)
                if (creds != null) {
                    return creds to "file at ${file.path}"
                }
            }
        }

        return null
    }

    private fun tryDecodeBase64Credentials(rawInput: String): GoogleCredentials? {
        val trimmed = rawInput.trim()

        // If user accidentally pasted raw JSON into the BASE64 variable
        if (trimmed.startsWith("{")) {
            return tryLoadJsonCredentials(trimmed)
        }

        // Try multiple decoding strategies to handle whitespace, mangled '+' characters, line breaks, etc.
        val strategies = listOf(
            // 1. Standard Base64 with spaces converted back to '+' (Fixes web form encoding issue)
            { Base64.getDecoder().decode(trimmed.replace(" ", "+").replace("\r", "").replace("\n", "")) },
            // 2. MIME Decoder (handles arbitrary line breaks and whitespace)
            { Base64.getMimeDecoder().decode(trimmed) },
            // 3. URL-safe Decoder
            { Base64.getUrlDecoder().decode(trimmed.replace(" ", "+").replace("\r", "").replace("\n", "")) },
            // 4. Standard Decoder directly
            { Base64.getDecoder().decode(trimmed) }
        )

        for (strategy in strategies) {
            try {
                val decodedBytes = strategy()
                val jsonStr = String(decodedBytes, Charsets.UTF_8)
                if (jsonStr.contains("service_account") || jsonStr.contains("private_key")) {
                    val creds = tryLoadJsonCredentials(jsonStr)
                    if (creds != null) return creds
                }
            } catch (ignored: Exception) {
                // Try next strategy
            }
        }
        return null
    }

    private fun tryLoadJsonCredentials(jsonStr: String): GoogleCredentials? {
        // Strategy A: Direct UTF-8 stream
        try {
            val stream = ByteArrayInputStream(jsonStr.toByteArray(Charsets.UTF_8))
            return GoogleCredentials.fromStream(stream)
        } catch (ignored: Exception) {
        }

        // Strategy B: Replace escaped newlines \\n with \n or fix json formatting
        try {
            val sanitized = jsonStr.replace("\\\\n", "\\n")
            val stream = ByteArrayInputStream(sanitized.toByteArray(Charsets.UTF_8))
            return GoogleCredentials.fromStream(stream)
        } catch (ignored: Exception) {
        }

        return null
    }

    private fun tryLoadFileCredentials(file: File): GoogleCredentials? {
        return try {
            val content = file.readText(Charsets.UTF_8)
            tryLoadJsonCredentials(content) ?: FileInputStream(file).use { GoogleCredentials.fromStream(it) }
        } catch (e: Exception) {
            logger.warn("Failed to load credentials from file ${file.path}: ${e.message}")
            null
        }
    }

    private fun ensureInitialized(): Boolean {
        if (FirebaseApp.getApps().isNotEmpty()) return true
        init()
        return FirebaseApp.getApps().isNotEmpty()
    }

    /**
     * Sends a push notification to all devices of a specific user.
     *
     * @param userId Target user ID
     * @param title Notification title
     * @param body Notification body
     * @param data Optional data payload for client-side handling
     */
    suspend fun sendPushToUser(
        userId: Int,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        if (!ensureInitialized()) {
            logger.warn("Firebase Admin is not initialized. Skipping push notification '$title' to user $userId.")
            return
        }

        val tokens = dbQuery {
            FcmTokenEntity.find { FcmTokensTable.userId eq userId }.map { it.token }
        }

        if (tokens.isEmpty()) {
            logger.warn("No FCM token registered for user $userId. Push notification '$title' skipped.")
            return
        }

        tokens.forEach { token ->
            sendToToken(token, title, body, data, userId)
        }
    }

    /**
     * Sends a push notification to multiple users.
     */
    suspend fun sendPushToUsers(
        userIds: List<Int>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        if (userIds.isEmpty() || !ensureInitialized()) return
        userIds.distinct().forEach { userId ->
            sendPushToUser(userId, title, body, data)
        }
    }

    /**
     * Sends a data-only FCM message to all ACCEPTED members of a plan room,
     * optionally excluding the user who triggered the action.
     *
     * Used to trigger background sync on room members' devices when
     * a task is created, updated, or deleted in the room.
     *
     * @param roomId The plan room ID
     * @param excludeUserId The user who performed the action (won't receive the notification)
     */
    suspend fun sendSyncTriggerToRoomMembers(roomId: String, excludeUserId: Int) {
        if (!ensureInitialized()) {
            logger.warn("Firebase Admin is not initialized. Skipping sync trigger for room $roomId.")
            return
        }

        val memberUserIds = dbQuery {
            PlanRoomMembersTable.selectAll().where {
                (PlanRoomMembersTable.roomId eq roomId) and
                (PlanRoomMembersTable.status eq RoomMemberStatus.ACCEPTED)
            }.map { it[PlanRoomMembersTable.userId] }
                .filter { it != excludeUserId }
        }

        if (memberUserIds.isEmpty()) return

        // Collect all tokens for all room members
        val allTokens = dbQuery {
            memberUserIds.flatMap { memberId ->
                FcmTokenEntity.find { FcmTokensTable.userId eq memberId }
                    .map { it.token to memberId }
            }
        }

        if (allTokens.isEmpty()) {
            logger.warn("No FCM tokens found for accepted members in room $roomId to send sync trigger")
            return
        }

        logger.info("Sending sync_tasks trigger to ${allTokens.size} devices in room $roomId")

        val apnsConfig = com.google.firebase.messaging.ApnsConfig.builder()
            .putHeader("apns-priority", "5")
            .putHeader("apns-push-type", "background")
            .setAps(
                com.google.firebase.messaging.Aps.builder()
                    .setContentAvailable(true)
                    .build()
            )
            .build()

        allTokens.forEach { (token, memberId) ->
            try {
                val message = Message.builder()
                    .setToken(token)
                    .setApnsConfig(apnsConfig)
                    .putData("type", "sync_tasks")
                    .putData("roomId", roomId)
                    .build()

                val response = FirebaseMessaging.getInstance().send(message)
                logger.info("Sync trigger sent to user $memberId: $response")
            } catch (e: FirebaseMessagingException) {
                handleFirebaseError(e, token, memberId)
            } catch (e: Exception) {
                logger.error("Error sending sync trigger to user $memberId: ${e.message}", e)
            }
        }
    }

    /**
     * Sends a push notification to a specific FCM token.
     * Handles invalid token cleanup automatically.
     */
    private suspend fun sendToToken(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
        userId: Int
    ) {
        try {
            val apnsConfig = com.google.firebase.messaging.ApnsConfig.builder()
                .putHeader("apns-priority", "10")
                .putHeader("apns-push-type", "alert")
                .setAps(
                    com.google.firebase.messaging.Aps.builder()
                        .setAlert(
                            com.google.firebase.messaging.ApsAlert.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build()
                        )
                        .setSound("default")
                        .setBadge(1)
                        .setContentAvailable(true)
                        .build()
                )
                .build()

            val androidConfig = com.google.firebase.messaging.AndroidConfig.builder()
                .setNotification(
                    com.google.firebase.messaging.AndroidNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setChannelId("planora_v2_channel")
                        .setSound("planora_chime")
                        .setColor("#7C4DFF")
                        .build()
                )
                .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                .build()

            val messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .putData("title", title)
                .putData("body", body)

            // Add custom data fields
            data.forEach { (key, value) ->
                messageBuilder.putData(key, value)
            }

            val response = FirebaseMessaging.getInstance().send(messageBuilder.build())
            logger.info("Successfully sent message to user $userId: $response")
        } catch (e: FirebaseMessagingException) {
            handleFirebaseError(e, token, userId)
        } catch (e: Exception) {
            logger.error("Error sending push notification to user $userId: ${e.message}", e)
        }
    }

    /**
     * Handles Firebase messaging errors.
     * Cleans up invalid/unregistered tokens from the database.
     */
    private suspend fun handleFirebaseError(
        e: FirebaseMessagingException,
        token: String,
        userId: Int
    ) {
        when (e.messagingErrorCode) {
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT -> {
                logger.warn("Removing invalid FCM token for user $userId: ${e.messagingErrorCode}")
                dbQuery {
                    FcmTokenEntity.find { FcmTokensTable.token eq token }
                        .firstOrNull()?.delete()
                }
            }
            else -> {
                logger.error("Firebase error for user $userId (${e.messagingErrorCode}): ${e.message}", e)
            }
        }
    }
}
