package com.yusufteker.planora.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.aakira.napier.Napier
import java.util.Calendar

/**
 * Schedules daily periodic alarms for Planora's Daily Digest & Overdue Reminders.
 *
 * Alarms are scheduled at 09:00, 12:00, and 16:00 every day using [AlarmManager.setInexactRepeating]
 * or exact alarms when precision is desired.
 */
object DailyDigestScheduler {

    private const val TAG = "DailyDigestScheduler"

    const val ACTION_DAILY_DIGEST = "com.yusufteker.planora.ACTION_DAILY_DIGEST"
    const val EXTRA_DIGEST_TYPE = "extra_digest_type"

    const val DIGEST_TYPE_MORNING = 1   // 09:00
    const val DIGEST_TYPE_OVERDUE = 2   // 12:00
    const val DIGEST_TYPE_AFTERNOON = 3 // 16:00

    private val DIGEST_HOURS = mapOf(
        DIGEST_TYPE_MORNING to 9,
        DIGEST_TYPE_OVERDUE to 12,
        DIGEST_TYPE_AFTERNOON to 16
    )

    /**
     * Schedules or refreshes repeating alarms for 09:00, 12:00, and 16:00 daily digests.
     *
     * @param context Android application context.
     */
    fun scheduleAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        DIGEST_HOURS.forEach { (digestType, hour) ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(ACTION_DAILY_DIGEST).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_DIGEST_TYPE, digestType)
            }

            val requestCode = 1000 + digestType
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                Napier.d("Scheduled daily digest type=$digestType for hour=$hour:00 at ${calendar.timeInMillis}", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to schedule daily digest type=$digestType: ${e.message}", tag = TAG)
            }
        }
    }
}
