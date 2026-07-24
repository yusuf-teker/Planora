package com.yusufteker.planora.core.snackbar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DefaultSnackbarManager : SnackbarManager {
    private val _messages = MutableStateFlow<SnackbarMessage?>(null)
    override val messages: StateFlow<SnackbarMessage?> = _messages.asStateFlow()

    override fun showMessage(message: String, type: SnackbarType) {
        _messages.update {
            SnackbarMessage(message = message, type = type)
        }
    }

    override fun clearMessage(id: Long) {
        _messages.update { current ->
            if (current?.id == id) null else current
        }
    }
}
