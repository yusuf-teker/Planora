package com.yusufteker.planora.feature.home.presentation.focus

data class FocusState(
    val taskId: String? = null,
    val taskTitle: String = "Odak Zamanı",
    val selectedDurationMinutes: Int = 25,
    val timeRemainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val isCompleting: Boolean = false
)
