package com.yusufteker.pulse.feature.home.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



class FocusViewModel(
    private val planRepository: PlanRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(FocusState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<FocusEffect>()
    val effect = _effect.asSharedFlow()

    private var timerJob: Job? = null

    fun loadTask(taskId: String?) {
        if (taskId == null) return
        viewModelScope.launch {
            _state.update { it.copy(taskId = taskId) }
            planRepository.observeAllTasks().collect { tasks ->
                val task = tasks.find { it.id == taskId }
                if (task != null) {
                    _state.update { it.copy(taskTitle = task.title) }
                }
            }
        }
    }

    fun setFocusDuration(minutes: Int) {
        if (_state.value.isRunning) return
        _state.update { 
            it.copy(
                selectedDurationMinutes = minutes,
                timeRemainingSeconds = minutes * 60,
                isFinished = false
            )
        }
    }

    fun toggleTimer() {
        if (_state.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_state.value.timeRemainingSeconds <= 0) return
        
        _state.update { it.copy(isRunning = true) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.timeRemainingSeconds > 0) {
                delay(1000)
                _state.update { it.copy(timeRemainingSeconds = it.timeRemainingSeconds - 1) }
            }
            _state.update { it.copy(isRunning = false, isFinished = true) }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _state.update { it.copy(isRunning = false) }
    }
    
    fun resetTimer() {
        timerJob?.cancel()
        _state.update { 
            it.copy(
                timeRemainingSeconds = it.selectedDurationMinutes * 60, 
                isRunning = false, 
                isFinished = false 
            ) 
        }
    }

    fun completeTask() {
        val taskId = _state.value.taskId ?: return
        if (_state.value.isCompleting) return

        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true) }
            val result = planRepository.completeTaskInstance(
                taskId = taskId,
                dateMs = getCurrentTimeMs(),
                isCompleted = true
            )
            _state.update { it.copy(isCompleting = false) }
            
            result.onSuccess {
                resetTimer()
                _effect.emit(FocusEffect.NavigateBack)
            }.onFailure {
                _effect.emit(FocusEffect.ShowSnackbar("Görev tamamlanamadı. Lütfen tekrar deneyin."))
            }
        }
    }
}
