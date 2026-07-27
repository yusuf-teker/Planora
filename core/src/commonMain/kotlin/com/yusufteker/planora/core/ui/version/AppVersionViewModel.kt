package com.yusufteker.planora.core.ui.version

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.domain.usecase.CheckAppVersionUseCase
import com.yusufteker.planora.core.domain.usecase.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        checkVersion()
    }

    /**
     * Triggers remote version check asynchronously.
     */
    fun checkVersion() {
        viewModelScope.launch {
            _updateStatus.value = checkAppVersionUseCase()
        }
    }
}
