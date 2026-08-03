package com.yusufteker.planora.android.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.yusufteker.planora.R

/**
 * Helper object for notifying Android Home Screen Widgets when task or calendar data changes.
 */
object PlanoraWidgetUpdater {

    const val ACTION_REFRESH_WIDGETS = "com.yusufteker.planora.ACTION_REFRESH_WIDGETS"
    const val ACTION_TOGGLE_VIEW_MODE = "com.yusufteker.planora.ACTION_TOGGLE_VIEW_MODE"
    const val EXTRA_VIEW_MODE = "extra_view_mode"
    const val EXTRA_SELECTED_DATE = "extra_selected_date"
    const val EXTRA_TASK_ID = "extra_task_id"

    /**
     * Broadcasts an intent to trigger immediate update of all Planora home screen widgets.
     */
    fun updateAllWidgets(context: Context) {
        try {
            val todayIntent = Intent(context, PlanoraTodayWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, PlanoraTodayWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(todayIntent)

            val manager = AppWidgetManager.getInstance(context)
            val todayIds = manager.getAppWidgetIds(ComponentName(context, PlanoraTodayWidgetProvider::class.java))
            if (todayIds.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(todayIds, R.id.widget_today_list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
