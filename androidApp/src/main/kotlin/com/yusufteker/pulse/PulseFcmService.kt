package com.yusufteker.pulse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class PulseFcmService : FirebaseMessagingService(), KoinComponent {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d("New FCM Token: $token", tag = "PulseFcmService")
        
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // Save the token locally
                val sessionPreferences: com.yusufteker.pulse.core.preferences.SessionPreferences by inject()
                sessionPreferences.saveFcmToken(token)

                // Try to send it to backend if logged in
                val authRepository: com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository by inject()
                if (authRepository.hasValidSession()) {
                    authRepository.registerFcmToken(token)
                }
            } catch (e: Exception) {
                Napier.e("Failed to handle new FCM token", e, tag = "PulseFcmService")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Napier.d("FCM Message Received: ${message.data}", tag = "PulseFcmService")

        val title = message.notification?.title ?: message.data["title"] ?: "Pulse"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        showNotification(title, body)

        // Arka planda verileri senkronize et ki diğer kullanıcı anında UI'da görsün.
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                //val planRepository: PlanRepository by inject()
                //planRepository.fetchMyTasks()
            } catch (e: Exception) {
                Napier.e("Failed to sync tasks on FCM message", e, tag = "PulseFcmService")
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "pulse_default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // You can change this to app logo
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pulse Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
