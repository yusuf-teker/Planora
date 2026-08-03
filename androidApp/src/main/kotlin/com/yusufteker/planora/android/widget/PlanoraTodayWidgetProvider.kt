package com.yusufteker.planora.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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
 *
 * Architecture:
 * - [updateAppWidget]: Full widget rebuild. Called on initial placement and periodic updates.
 * - [applyModeToViews]: Applies tab visibility and header text to a RemoteViews based on mode.
 * - Toggle (onReceive): Uses [partiallyUpdateAppWidget] for smooth tab switching
 *   WITHOUT rebuilding the entire widget (avoids list adapter reset and flicker).
 */
class PlanoraTodayWidgetProvider : AppWidgetProvider(), KoinComponent {

    companion object {
        private const val PREFS_NAME = "com.yusufteker.planora.today_widget_prefs"
        private const val KEY_VIEW_MODE = "view_mode_"
        private const val KEY_GLOBAL_VIEW_MODE = "view_mode_global"
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
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val newMode = intent.getStringExtra(PlanoraWidgetUpdater.EXTRA_VIEW_MODE) ?: MODE_TODAY

            // Save mode to SharedPreferences (commit synchronously so it's visible immediately)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(KEY_GLOBAL_VIEW_MODE, newMode)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                editor.putString(KEY_VIEW_MODE + appWidgetId, newMode)
                editor.commit()
                // Full rebuild — guarantees tab visibility is applied reliably.
                // setRemoteAdapter with the same stable URI reuses the existing adapter,
                // so no flicker/reset occurs while the list data refreshes.
                updateAppWidget(context, appWidgetManager, appWidgetId)
            } else {
                val ids = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, PlanoraTodayWidgetProvider::class.java)
                )
                for (id in ids) {
                    editor.putString(KEY_VIEW_MODE + id, newMode)
                }
                editor.commit()
                for (id in ids) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    /**
     * Full widget rebuild. Called from [onUpdate] on initial placement, periodic updates,
     * and tab toggling.
     *
     * Uses a full [AppWidgetManager.updateAppWidget] so the tab visibility is applied
     * reliably (unlike `partiallyUpdateAppWidget`, which some launchers apply
     * inconsistently for `setViewVisibility`). Re-binding the RemoteAdapter with the
     * same stable URI reuses the existing adapter, so switching modes refreshes the
     * list data without a full rebuild/flicker.
     */
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_today_tasks)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_VIEW_MODE + appWidgetId, null)
            ?: prefs.getString(KEY_GLOBAL_VIEW_MODE, MODE_TODAY)
            ?: MODE_TODAY

        // Apply tab visibility and header text
        applyModeToViews(context, views, mode)

        // Tab click intents
        setupTabClickIntents(context, views, appWidgetId)

        // Header click intent
        setupHeaderClickIntent(context, views, appWidgetId, mode)

        // Set up ListView RemoteAdapter with stable URI per widget instance
        val adapterIntent = Intent(context, TodayTasksRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("planora://today_widget/$appWidgetId")
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

        // Full update — replaces the entire widget RemoteViews
        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_today_list)
    }

    /**
     * Applies tab selection (background + text color) and header/date text on the given
     * RemoteViews based on [mode]. Uses a single tab set whose selection is driven by
     * [RemoteViews.setBackgroundResource] and [RemoteViews.setTextColor] — this is reliable
     * on all launchers (unlike `setViewVisibility`) and keeps the switch's position and
     * size constant between toggles.
     */
    private fun applyModeToViews(context: Context, views: RemoteViews, mode: String) {
        val cal = Calendar.getInstance()
        val dfs = DateFormatSymbols(Locale.getDefault())

        if (mode == MODE_WEEK) {
            // Mode: Bu Hafta
            views.setTextViewText(R.id.widget_today_header, context.getString(R.string.widget_this_week))
            views.setTextViewText(R.id.widget_empty_text, context.getString(R.string.widget_no_tasks_this_week))

            var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
            if (dayOfWeek < 0) dayOfWeek += 7

            val weekStartCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -dayOfWeek) }
            val weekEndCal = (weekStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }

            val startDay = weekStartCal.get(Calendar.DAY_OF_MONTH)
            val startMonth = dfs.months[weekStartCal.get(Calendar.MONTH)].replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val endDay = weekEndCal.get(Calendar.DAY_OF_MONTH)
            val endMonth = dfs.months[weekEndCal.get(Calendar.MONTH)].replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }

            val weekSubText = if (weekStartCal.get(Calendar.MONTH) == weekEndCal.get(Calendar.MONTH)) {
                "$startDay - $endDay $startMonth"
            } else {
                "$startDay $startMonth - $endDay $endMonth"
            }
            views.setTextViewText(R.id.widget_today_date_sub, weekSubText)

            // Bugün tab: unselected
            views.setInt(R.id.btn_mode_today, "setBackgroundResource", R.drawable.widget_tab_unselected)
            views.setTextColor(R.id.btn_mode_today, android.graphics.Color.parseColor("#9CA3AF"))
            // Bu Hafta tab: selected
            views.setInt(R.id.btn_mode_week, "setBackgroundResource", R.drawable.widget_tab_selected)
            views.setTextColor(R.id.btn_mode_week, android.graphics.Color.parseColor("#FFFFFF"))
        } else {
            // Mode: Bugün
            views.setTextViewText(R.id.widget_today_header, context.getString(R.string.widget_today_name))
            views.setTextViewText(R.id.widget_empty_text, context.getString(R.string.widget_no_tasks_today))

            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val monthName = dfs.months[cal.get(Calendar.MONTH)].replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val dayOfWeekName = dfs.weekdays[cal.get(Calendar.DAY_OF_WEEK)].replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            views.setTextViewText(R.id.widget_today_date_sub, "$dayOfMonth $monthName, $dayOfWeekName")

            // Bugün tab: selected
            views.setInt(R.id.btn_mode_today, "setBackgroundResource", R.drawable.widget_tab_selected)
            views.setTextColor(R.id.btn_mode_today, android.graphics.Color.parseColor("#FFFFFF"))
            // Bu Hafta tab: unselected
            views.setInt(R.id.btn_mode_week, "setBackgroundResource", R.drawable.widget_tab_unselected)
            views.setTextColor(R.id.btn_mode_week, android.graphics.Color.parseColor("#9CA3AF"))
        }
    }

    /**
     * Registers PendingIntents for the Today/Week toggle tab buttons.
     *
     * Each PendingIntent has a unique data URI so Android's PendingIntent.filterEquals()
     * correctly treats them as distinct intents (preventing one from overwriting the other).
     */
    private fun setupTabClickIntents(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Today tab intent
        val todayTabIntent = Intent(context, PlanoraTodayWidgetProvider::class.java).apply {
            action = PlanoraWidgetUpdater.ACTION_TOGGLE_VIEW_MODE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(PlanoraWidgetUpdater.EXTRA_VIEW_MODE, MODE_TODAY)
            data = Uri.parse("planora://widget_mode/${appWidgetId}/today")
        }
        val todayTabPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId * 10 + 1, todayTabIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_mode_today, todayTabPendingIntent)

        // Week tab intent
        val weekTabIntent = Intent(context, PlanoraTodayWidgetProvider::class.java).apply {
            action = PlanoraWidgetUpdater.ACTION_TOGGLE_VIEW_MODE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(PlanoraWidgetUpdater.EXTRA_VIEW_MODE, MODE_WEEK)
            data = Uri.parse("planora://widget_mode/${appWidgetId}/week")
        }
        val weekTabPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId * 10 + 2, weekTabIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_mode_week, weekTabPendingIntent)
    }

    /**
     * Registers the header click intent that opens MainActivity with the current view mode.
     */
    private fun setupHeaderClickIntent(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        mode: String
    ) {
        val headerAppIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PlanoraWidgetUpdater.EXTRA_VIEW_MODE, mode)
        }
        val headerPendingIntent = PendingIntent.getActivity(
            context, appWidgetId * 10 + 4, headerAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header_title_container, headerPendingIntent)
    }
}
