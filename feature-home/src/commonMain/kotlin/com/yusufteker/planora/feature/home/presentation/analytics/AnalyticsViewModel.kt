package com.yusufteker.planora.feature.home.presentation.analytics

import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel powering the Productivity & Insights Screen.
 *
 * Observes local tasks from [PlanRepository] and user subscription from [SessionPreferences].
 * Aggregates task completion metrics, weekly trends, streaks, and category breakdowns.
 *
 * @param planRepository Data access layer for offline tasks.
 * @param sessionPreferences Preferences providing reactive Premium status.
 * @param timeProvider Clock supplier returning current epoch milliseconds (customizable for unit tests).
 */
class AnalyticsViewModel(
    private val planRepository: PlanRepository,
    private val sessionPreferences: SessionPreferences,
    private val timeProvider: () -> Long
) : BaseViewModel<AnalyticsState, AnalyticsEvent, AnalyticsEffect>(AnalyticsState()) {

    constructor(
        planRepository: PlanRepository,
        sessionPreferences: SessionPreferences
    ) : this(
        planRepository = planRepository,
        sessionPreferences = sessionPreferences,
        timeProvider = { com.yusufteker.planora.core.utils.getCurrentTimeMs() }
    )

    init {
        launch {
            try {
                combine(
                    planRepository.observeAllTasks(),
                    sessionPreferences.isPremiumFlow
                ) { tasks, isPrem ->
                    try {
                        computeAnalytics(tasks, isPrem)
                    } catch (e: Exception) {
                        AnalyticsState(isLoading = false, isPremium = isPrem)
                    }
                }.collect { calculatedState ->
                    setState { calculatedState }
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
            }
        }
    }

    override fun onEvent(event: AnalyticsEvent) {
        when (event) {
            AnalyticsEvent.BackClicked -> setEffect(AnalyticsEffect.NavigateBack)
            AnalyticsEvent.UpgradeClicked -> setEffect(AnalyticsEffect.NavigateToPremium)
        }
    }

    private fun computeAnalytics(tasks: List<TaskDto>, isPremium: Boolean): AnalyticsState {
        val total = tasks.size
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val rate = if (total > 0) completed.toFloat() / total else 0f

        val priorityMap = tasks.groupBy { (it.specificDetails as? ItemDetails.Task)?.priority ?: TaskPriority.MEDIUM }.mapValues { it.value.size }
        val typeMap = tasks.groupBy { it.type }.mapValues { it.value.size }

        val currentMs = timeProvider()
        val weekly = computeWeeklyProductivity(tasks, currentMs)
        val streak = computeStreak(tasks, currentMs)
        val peakHour = computePeakHour(tasks)

        // PRO-only deep analytics
        val monthly = if (isPremium) computeMonthlyTrend(tasks, currentMs) else emptyList()
        val longestStreak = if (isPremium) computeLongestStreak(tasks, currentMs) else 0
        val bestWorstDay = if (isPremium) computeBestWorstDay(tasks) else Pair("", "")
        val avgDaily = if (isPremium && monthly.isNotEmpty()) {
            monthly.map { it.completedCount }.average().toFloat()
        } else 0f

        return AnalyticsState(
            isLoading = false,
            isPremium = isPremium,
            totalTasks = total,
            completedTasks = completed,
            completionRate = rate,
            currentStreakDays = streak,
            weeklyProductivity = weekly,
            priorityBreakdown = priorityMap,
            typeBreakdown = typeMap,
            peakHourText = peakHour,
            // PRO fields
            monthlyTrend = monthly,
            longestStreakDays = longestStreak,
            bestDayLabel = bestWorstDay.first,
            worstDayLabel = bestWorstDay.second,
            avgDailyCompletions = avgDaily
        )
    }

    /**
     * Son 30 günün günlük tamamlama verilerini hesaplar (PRO).
     */
    private fun computeMonthlyTrend(tasks: List<TaskDto>, currentMs: Long): List<DayProductivity> {
        val dayMs = 24L * 60L * 60L * 1000L
        val timeZone = TimeZone.currentSystemDefault()
        val days = mutableListOf<DayProductivity>()
        for (i in 29 downTo 0) {
            val targetMs = currentMs - (i * dayMs)
            val targetInstant = Instant.fromEpochMilliseconds(targetMs)
            val targetDt = targetInstant.toLocalDateTime(timeZone)
            val dayTasks = tasks.filter { task ->
                try {
                    val taskDt = Instant.fromEpochMilliseconds(task.startTime).toLocalDateTime(timeZone)
                    taskDt.year == targetDt.year &&
                        taskDt.month == targetDt.month &&
                        taskDt.dayOfMonth == targetDt.dayOfMonth
                } catch (_: Exception) { false }
            }
            val dayLabel = "${targetDt.dayOfMonth}/${targetDt.monthNumber}"
            days.add(
                DayProductivity(
                    dayLabel = dayLabel,
                    completedCount = dayTasks.count { it.status == TaskStatus.COMPLETED },
                    totalCount = dayTasks.size,
                    isToday = i == 0
                )
            )
        }
        return days
    }

    /**
     * Tüm geçmiş içindeki en uzun streak'i hesaplar (PRO).
     * Mevcut [computeStreak] yalnızca güncel streak'i döner; bu fonksiyon tarihsel maksimumu bulur.
     */
    private fun computeLongestStreak(tasks: List<TaskDto>, currentMs: Long): Int {
        val dayMs = 24L * 60L * 60L * 1000L
        val timeZone = TimeZone.currentSystemDefault()
        val completedDays = tasks
            .filter { it.status == TaskStatus.COMPLETED }
            .mapNotNull { task ->
                try {
                    val dt = Instant.fromEpochMilliseconds(task.startTime).toLocalDateTime(timeZone)
                    "${dt.year}-${dt.monthNumber}-${dt.dayOfMonth}"
                } catch (_: Exception) { null }
            }.sorted().toSet()

        if (completedDays.isEmpty()) return 0

        // Build a Set<Long> of normalised day epoch values for fast lookup
        val dayEpochs = tasks
            .filter { it.status == TaskStatus.COMPLETED }
            .mapNotNull { task ->
                try {
                    val ms = task.startTime
                    val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(timeZone)
                    // Normalise to start-of-day by stripping hours/min/sec
                    val dayKey = "${dt.year}-${dt.monthNumber}-${dt.dayOfMonth}"
                    dayKey
                } catch (_: Exception) { null }
            }.toSet()

        // Walk from earliest completed day forward, counting consecutive days
        // Use a range from (currentMs - 365 days) to today to bound computation
        var maxStreak = 0
        var runStreak = 0
        for (i in 365 downTo 0) {
            val checkMs = currentMs - (i * dayMs)
            val dt = try { Instant.fromEpochMilliseconds(checkMs).toLocalDateTime(timeZone) } catch (_: Exception) { continue }
            val key = "${dt.year}-${dt.monthNumber}-${dt.dayOfMonth}"
            if (dayEpochs.contains(key)) {
                runStreak++
                if (runStreak > maxStreak) maxStreak = runStreak
            } else {
                runStreak = 0
            }
        }
        return maxStreak
    }

    /**
     * Son 30 günde hangi haftanın gününün en verimli / en düşük tamamlamaya sahip olduğunu döner.
     * @return Pair(bestDayName, worstDayName) — ör. Pair("Monday", "Sunday")
     */
    private fun computeBestWorstDay(tasks: List<TaskDto>): Pair<String, String> {
        val timeZone = TimeZone.currentSystemDefault()
        val dayMs = 24L * 60L * 60L * 1000L
        val currentMs = timeProvider()
        val last30Start = currentMs - (30 * dayMs)

        // Only completed tasks in last 30 days
        val completedByDow = mutableMapOf<String, Int>()
        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        daysOfWeek.forEach { completedByDow[it] = 0 }

        tasks.filter { it.status == TaskStatus.COMPLETED && it.startTime >= last30Start }
            .forEach { task ->
                try {
                    val dt = Instant.fromEpochMilliseconds(task.startTime).toLocalDateTime(timeZone)
                    val dow = dt.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                    completedByDow[dow] = (completedByDow[dow] ?: 0) + 1
                } catch (_: Exception) {}
            }

        val best = completedByDow.maxByOrNull { it.value }?.key ?: ""
        val worst = completedByDow.minByOrNull { it.value }?.key ?: ""
        return Pair(best, worst)
    }

    /**
     * Tamamlanan görevlerin saate göre dağılımını analiz ederek zirve saat aralığını döner.
     * Sonuç "HH:00 - HH:59" formatındadır.
     */
    private fun computePeakHour(tasks: List<TaskDto>): String {
        val timeZone = TimeZone.currentSystemDefault()
        val hourCounts = mutableMapOf<Int, Int>()
        tasks.filter { it.status == TaskStatus.COMPLETED }.forEach { task ->
            try {
                val dt = Instant.fromEpochMilliseconds(task.startTime).toLocalDateTime(timeZone)
                hourCounts[dt.hour] = (hourCounts[dt.hour] ?: 0) + 1
            } catch (_: Exception) {}
        }
        if (hourCounts.isEmpty()) return "09:00 - 12:00"
        val peakHour = hourCounts.maxByOrNull { it.value }?.key ?: 9
        val hourStr = peakHour.toString().padStart(2, '0')
        return "$hourStr:00 - $hourStr:59"
    }

    private fun computeWeeklyProductivity(tasks: List<TaskDto>, currentMs: Long): List<DayProductivity> {
        val dayMs = 24L * 60L * 60L * 1000L
        val timeZone = TimeZone.currentSystemDefault()
        val currentInstant = Instant.fromEpochMilliseconds(currentMs)
        val currentDateTime = currentInstant.toLocalDateTime(timeZone)

        val days = mutableListOf<DayProductivity>()
        for (i in 6 downTo 0) {
            val targetMs = currentMs - (i * dayMs)
            val targetInstant = Instant.fromEpochMilliseconds(targetMs)
            val targetDateTime = targetInstant.toLocalDateTime(timeZone)

            val dayLabel = when (targetDateTime.dayOfWeek.name.take(3).uppercase()) {
                "MON" -> "Pzt"
                "TUE" -> "Sal"
                "WED" -> "Çrş"
                "THU" -> "Per"
                "FRI" -> "Cum"
                "SAT" -> "Cmt"
                "SUN" -> "Paz"
                else -> targetDateTime.dayOfWeek.name.take(3)
            }

            // Tasks active on this target day (by startTime)
            val dayTasks = tasks.filter { task ->
                try {
                    val taskDateMs = task.startTime
                    val taskDateTime = Instant.fromEpochMilliseconds(taskDateMs).toLocalDateTime(timeZone)
                    taskDateTime.year == targetDateTime.year &&
                            taskDateTime.month == targetDateTime.month &&
                            taskDateTime.dayOfMonth == targetDateTime.dayOfMonth
                } catch (_: Exception) {
                    false
                }
            }

            val completedCount = dayTasks.count { it.status == TaskStatus.COMPLETED }
            val isToday = i == 0

            days.add(
                DayProductivity(
                    dayLabel = dayLabel,
                    completedCount = completedCount,
                    totalCount = dayTasks.size,
                    isToday = isToday
                )
            )
        }
        return days
    }

    private fun computeStreak(tasks: List<TaskDto>, currentMs: Long): Int {
        val dayMs = 24L * 60L * 60L * 1000L
        val timeZone = TimeZone.currentSystemDefault()

        // Set of days (as "YYYY-MM-DD") where at least 1 task was completed
        val completedDays = tasks
            .filter { it.status == TaskStatus.COMPLETED }
            .mapNotNull { task ->
                try {
                    val ms = task.startTime
                    val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(timeZone)
                    "${dt.year}-${dt.month.name}-${dt.dayOfMonth}"
                } catch (_: Exception) {
                    null
                }
            }.toSet()

        if (completedDays.isEmpty()) return 0

        var streak = 0
        var checkMs = currentMs

        try {
            // Check today first; if not today, check starting from yesterday
            val todayDt = Instant.fromEpochMilliseconds(currentMs).toLocalDateTime(timeZone)
            val todayKey = "${todayDt.year}-${todayDt.month.name}-${todayDt.dayOfMonth}"

            if (completedDays.contains(todayKey)) {
                streak++
                checkMs -= dayMs
            } else {
                // Check yesterday
                val yesterdayDt = Instant.fromEpochMilliseconds(currentMs - dayMs).toLocalDateTime(timeZone)
                val yesterdayKey = "${yesterdayDt.year}-${yesterdayDt.month.name}-${yesterdayDt.dayOfMonth}"
                if (!completedDays.contains(yesterdayKey)) {
                    return 0
                }
                streak++
                checkMs = currentMs - (2 * dayMs)
            }

            // Loop backwards day by day (bounded to max completed days or 365 days)
            var safetyCounter = 0
            while (safetyCounter < 365 && safetyCounter < completedDays.size) {
                safetyCounter++
                val dt = Instant.fromEpochMilliseconds(checkMs).toLocalDateTime(timeZone)
                val key = "${dt.year}-${dt.month.name}-${dt.dayOfMonth}"
                if (completedDays.contains(key)) {
                    streak++
                    checkMs -= dayMs
                } else {
                    break
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully on parsing issue
        }

        return streak
    }
}
