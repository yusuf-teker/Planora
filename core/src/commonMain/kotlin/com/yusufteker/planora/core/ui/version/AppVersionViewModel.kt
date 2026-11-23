package com.yusufteker.planora.core.ui.version

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.domain.usecase.CheckAppVersionUseCase
import com.yusufteker.planora.core.domain.usecase.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel responsible for triggering application version check and maintaining update status state.
 *
 * @property checkAppVersionUseCase UseCase that evaluates installed app version vs remote server bounds.
 */
class AppVersionViewModel(
    private val checkAppVersionUseCase: CheckAppVersionUseCase
) : ViewModel() {

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.UpToDate)

    /**
     * Observable [StateFlow] emitting current [UpdateStatus].
     */
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    init {
        // Defer non-blocking version check until after first frame settles
        checkVersion(initialDelayMs = 1500L)
    }

    /**
     * Triggers remote version check asynchronously.
     */
    fun checkVersion(initialDelayMs: Long = 0L) {
        viewModelScope.launch {
            if (initialDelayMs > 0L) {
                delay(initialDelayMs)
            }
            _updateStatus.value = withContext(Dispatchers.IO) {
                checkAppVersionUseCase()
            }
        }
    }
}
