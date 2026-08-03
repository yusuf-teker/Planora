package com.yusufteker.planora.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.yusufteker.planora.R
import com.yusufteker.planora.core.database.PlanoraDatabase
import org.koin.core.component.KoinComponent
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

class TodayTasksRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayTasksRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TodayTasksRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory, KoinComponent {

    private val PREFS_NAME = "com.yusufteker.planora.today_widget_prefs"
    private val KEY_VIEW_MODE = "view_mode_"

    private val database: PlanoraDatabase?
        get() = try {
            org.koin.core.context.GlobalContext.getOrNull()?.get<PlanoraDatabase>()
        } catch (e: Throwable) {
            null
        }
    private val taskItems = mutableListOf<TodayTaskItem>()

    data class TodayTaskItem(
        val id: String,
        val title: String,
        val timeString: String,
        val type: String,
        val isCompleted: Boolean
    )

    override fun onCreate() {}

    override fun onDataSetChanged() {
        taskItems.clear()
        try {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val mode = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                prefs.getString(KEY_VIEW_MODE + appWidgetId, "TODAY") ?: "TODAY"
            } else "TODAY"

            val db = database ?: return
            
            val todayCal = Calendar.getInstance()
            val todayStr = formatDate(todayCal)

            // Week boundaries
            var dayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
            if (dayOfWeek < 0) dayOfWeek += 7

            val weekStartCal = (todayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -dayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val weekEndCal = (weekStartCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            val dfs = DateFormatSymbols(Locale.getDefault())
            val entities = db.planoraDatabaseQueries.getAllTasks().executeAsList()

            for (entity in entities) {
                if (entity.type == "NOTE" || entity.type == "FOLDER" || entity.parentId != null) continue

                var actualStartTime = entity.startTime
                var actualEndTime = entity.endTime ?: entity.startTime

                if (entity.type == "TASK" && entity.specificDetails != null) {
                    try {
                        val json = org.json.JSONObject(entity.specificDetails)
                        if (json.has("deadline") && !json.isNull("deadline")) {
                            val deadline = json.getLong("deadline")
                            actualStartTime = deadline
                            actualEndTime = deadline
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                val startCal = Calendar.getInstance().apply { timeInMillis = actualStartTime }
                val endCal = Calendar.getInstance().apply { timeInMillis = actualEndTime }

                val isMatch = if (mode == "WEEK") {
                    // Check if item falls anywhere inside current week
                    val itemDateStr = formatDate(startCal)
                    val weekStartDateStr = formatDate(weekStartCal)
                    val weekEndDateStr = formatDate(weekEndCal)
                    (itemDateStr >= weekStartDateStr && itemDateStr <= weekEndDateStr) ||
                    (startCal.timeInMillis <= weekEndCal.timeInMillis && endCal.timeInMillis >= weekStartCal.timeInMillis)
                } else {
                    // Check if item falls on today's date
                    var matchesToday = false
                    val currCal = startCal.clone() as Calendar
                    while (currCal.timeInMillis <= endCal.timeInMillis || formatDate(currCal) == formatDate(endCal)) {
                        if (formatDate(currCal) == todayStr) {
                            matchesToday = true
                            break
                        }
                        if (formatDate(currCal) == formatDate(endCal)) break
                        currCal.add(Calendar.DAY_OF_MONTH, 1)
                    }
                    matchesToday
                }

                if (isMatch) {
                    val rawTimeStr = if (entity.isAllDay == 1L) {
                        context.getString(R.string.widget_all_day)
                    } else if (entity.type == "EVENT") {
                        val startDt = Calendar.getInstance().apply { timeInMillis = actualStartTime }
                        val startHour = String.format(Locale.US, "%02d", startDt.get(Calendar.HOUR_OF_DAY))
                        val startMin = String.format(Locale.US, "%02d", startDt.get(Calendar.MINUTE))
                        
                        if (actualEndTime > actualStartTime) {
                            val endDt = Calendar.getInstance().apply { timeInMillis = actualEndTime }
                            val endHour = String.format(Locale.US, "%02d", endDt.get(Calendar.HOUR_OF_DAY))
                            val endMin = String.format(Locale.US, "%02d", endDt.get(Calendar.MINUTE))
                            "$startHour:$startMin - $endHour:$endMin"
                        } else {
                            "$startHour:$startMin"
                        }
                    } else {
                        val dt = Calendar.getInstance().apply { timeInMillis = actualStartTime }
                        val hour = String.format(Locale.US, "%02d", dt.get(Calendar.HOUR_OF_DAY))
                        val min = String.format(Locale.US, "%02d", dt.get(Calendar.MINUTE))
                        "$hour:$min"
                    }

                    val finalTimeStr = if (mode == "WEEK") {
                        val shortDay = dfs.shortWeekdays[startCal.get(Calendar.DAY_OF_WEEK)].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        "$shortDay • $rawTimeStr"
                    } else {
                        rawTimeStr
                    }

                    val isCompleted = entity.status == "COMPLETED"

                    taskItems.add(
                        TodayTaskItem(
                            id = entity.id,
                            title = entity.title,
                            timeString = finalTimeStr,
                            type = entity.type,
                            isCompleted = isCompleted
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("WidgetCrash", "Error in TodayTasksRemoteViewsService", e)
            e.printStackTrace()
        }
    }

    private fun formatDate(cal: Calendar): String {
        return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    override fun onDestroy() {
        taskItems.clear()
    }

    override fun getCount(): Int = taskItems.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        if (position >= taskItems.size) return views

        val item = taskItems[position]
        views.setTextViewText(R.id.widget_task_title, item.title)
        views.setTextViewText(R.id.widget_task_time, item.timeString)

        if (item.type == "EVENT") {
            views.setInt(R.id.widget_task_status_icon, "setImageResource", R.drawable.ic_widget_event)
            views.setInt(R.id.widget_task_status_icon, "setColorFilter", 0)
            views.setTextColor(R.id.widget_task_title, Color.parseColor("#FEF3C7"))
            views.setTextColor(R.id.widget_task_time, Color.parseColor("#F59E0B"))
        } else {
            if (item.isCompleted) {
                views.setTextColor(R.id.widget_task_title, Color.parseColor("#6B7280"))
                views.setTextColor(R.id.widget_task_time, Color.parseColor("#6B7280"))
                views.setInt(R.id.widget_task_status_icon, "setImageResource", R.drawable.ic_widget_task_on)
                views.setInt(R.id.widget_task_status_icon, "setColorFilter", 0)
            } else {
                views.setTextColor(R.id.widget_task_title, Color.parseColor("#F3F4F6"))
                views.setTextColor(R.id.widget_task_time, Color.parseColor("#9CA3AF"))
                views.setInt(R.id.widget_task_status_icon, "setImageResource", R.drawable.ic_widget_task_off)
                views.setInt(R.id.widget_task_status_icon, "setColorFilter", 0)
            }
        }

        val fillInIntent = Intent().apply {
            putExtra(PlanoraWidgetUpdater.EXTRA_TASK_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_task_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
