package com.yusufteker.planora.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import com.yusufteker.planora.MainActivity
import com.yusufteker.planora.R
import org.koin.core.component.KoinComponent
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

/**
 * BroadcastReceiver for the 2x2/3x2 Today & Weekly Tasks AppWidget.
 *
 * Supports toggling between Today and Weekly views via interactive top tabs.
 */
class PlanoraTodayWidgetProvider : AppWidgetProvider(), KoinComponent {

    companion object {
        private const val PREFS_NAME = "com.yusufteker.planora.today_widget_prefs"
        private const val KEY_VIEW_MODE = "view_mode_"
        const val MODE_TODAY = "TODAY"
        const val MODE_WEEK = "WEEK"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == PlanoraWidgetUpdater.ACTION_TOGGLE_VIEW_MODE) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val newMode = intent.getStringExtra(PlanoraWidgetUpdater.EXTRA_VIEW_MODE) ?: MODE_TODAY
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_VIEW_MODE + appWidgetId, newMode).commit()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_today_tasks)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_VIEW_MODE + appWidgetId, MODE_TODAY) ?: MODE_TODAY

        val cal = Calendar.getInstance()
        val dfs = DateFormatSymbols(Locale.getDefault())

        if (mode == MODE_WEEK) {
            // Mode: Bu Hafta
            views.setTextViewText(R.id.widget_today_header, context.getString(R.string.widget_this_week))
            
            var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
            if (dayOfWeek < 0) dayOfWeek += 7

            val weekStartCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -dayOfWeek) }
            val weekEndCal = (weekStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }

            val startDay = weekStartCal.get(Calendar.DAY_OF_MONTH)
            val startMonth = dfs.months[weekStartCal.get(Calendar.MONTH)].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val endDay = weekEndCal.get(Calendar.DAY_OF_MONTH)
            val endMonth = dfs.months[weekEndCal.get(Calendar.MONTH)].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val weekSubText = if (weekStartCal.get(Calendar.MONTH) == weekEndCal.get(Calendar.MONTH)) {
                "$startDay - $endDay $startMonth"
            } else {
                "$startDay $startMonth - $endDay $endMonth"
            }
            views.setTextViewText(R.id.widget_today_date_sub, weekSubText)

            // Tab styles
            views.setInt(R.id.btn_mode_today, "setBackgroundResource", R.drawable.widget_tab_unselected)
            views.setTextColor(R.id.btn_mode_today, Color.parseColor("#9CA3AF"))
            views.setInt(R.id.btn_mode_week, "setBackgroundResource", R.drawable.widget_tab_selected)
            views.setTextColor(R.id.btn_mode_week, Color.WHITE)
        } else {
            // Mode: Bugün
            views.setTextViewText(R.id.widget_today_header, context.getString(R.string.widget_today_name))
            
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val monthName = dfs.months[cal.get(Calendar.MONTH)].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val dayOfWeekName = dfs.weekdays[cal.get(Calendar.DAY_OF_WEEK)].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            views.setTextViewText(R.id.widget_today_date_sub, "$dayOfMonth $monthName, $dayOfWeekName")

            // Tab styles
            views.setInt(R.id.btn_mode_today, "setBackgroundResource", R.drawable.widget_tab_selected)
            views.setTextColor(R.id.btn_mode_today, Color.WHITE)
            views.setInt(R.id.btn_mode_week, "setBackgroundResource", R.drawable.widget_tab_unselected)
            views.setTextColor(R.id.btn_mode_week, Color.parseColor("#9CA3AF"))
        }

        // Toggle Tab Click Intents
        val todayTabIntent = Intent(context, PlanoraTodayWidgetProvider::class.java).apply {
            action = PlanoraWidgetUpdater.ACTION_TOGGLE_VIEW_MODE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(PlanoraWidgetUpdater.EXTRA_VIEW_MODE, MODE_TODAY)
        }
        val todayTabPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId * 10 + 1, todayTabIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_mode_today, todayTabPendingIntent)

        val weekTabIntent = Intent(context, PlanoraTodayWidgetProvider::class.java).apply {
            action = PlanoraWidgetUpdater.ACTION_TOGGLE_VIEW_MODE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(PlanoraWidgetUpdater.EXTRA_VIEW_MODE, MODE_WEEK)
        }
        val weekTabPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId * 10 + 2, weekTabIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_mode_week, weekTabPendingIntent)

        // Set up ListView RemoteAdapter
        val adapterIntent = Intent(context, TodayTasksRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_today_list, adapterIntent)
        views.setEmptyView(R.id.widget_today_list, R.id.widget_today_empty)

        // PendingIntent for clicking task items -> opens MainActivity
        val appIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context, appWidgetId * 10 + 3, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_today_list, appPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_today_list)
    }
}
