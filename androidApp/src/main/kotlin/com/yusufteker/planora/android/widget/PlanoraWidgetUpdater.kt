package com.yusufteker.planora.android.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.yusufteker.planora.R

/**
 * Helper object for notifying Android Home Screen Widgets when task or calendar data changes.
 *
 * Uses [AppWidgetManager.notifyAppWidgetViewDataChanged] to refresh list data
 * without triggering a full widget rebuild (which would cause visible flicker
 * and reset tab toggle state).
 */
object PlanoraWidgetUpdater {

    const val ACTION_REFRESH_WIDGETS = "com.yusufteker.planora.ACTION_REFRESH_WIDGETS"
    const val ACTION_TOGGLE_VIEW_MODE = "com.yusufteker.planora.ACTION_TOGGLE_VIEW_MODE"
    const val EXTRA_VIEW_MODE = "extra_view_mode"
    const val EXTRA_SELECTED_DATE = "extra_selected_date"
    const val EXTRA_TASK_ID = "extra_task_id"

    /**
     * Refreshes all Planora home screen widgets' list data.
     *
     * This only triggers [android.widget.RemoteViewsService.RemoteViewsFactory.onDataSetChanged]
     * to reload task items — it does NOT rebuild the widget layout, so the toggle tab
     * state is preserved and there's no visible flicker.
     */
    fun updateAllWidgets(context: Context) {
        try {
            val manager = AppWidgetManager.getInstance(context)
            val todayIds = manager.getAppWidgetIds(
                ComponentName(context, PlanoraTodayWidgetProvider::class.java)
            )
            if (todayIds.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(todayIds, R.id.widget_today_list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
