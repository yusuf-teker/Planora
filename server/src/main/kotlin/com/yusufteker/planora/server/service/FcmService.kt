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
        val tokens = dbQuery {
            FcmTokenEntity.find { FcmTokensTable.userId eq userId }.map { it.token }
        }

        if (tokens.isEmpty()) return

        tokens.forEach { token ->
            sendToToken(token, title, body, data, userId)
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

        logger.info("Sending sync_tasks trigger to ${allTokens.size} devices in room $roomId")

        allTokens.forEach { (token, memberId) ->
            try {
                val message = Message.builder()
                    .setToken(token)
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
                        .build()
                )
                .build()

            val androidConfig = com.google.firebase.messaging.AndroidConfig.builder()
                .setNotification(
                    com.google.firebase.messaging.AndroidNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setSound("default")
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
