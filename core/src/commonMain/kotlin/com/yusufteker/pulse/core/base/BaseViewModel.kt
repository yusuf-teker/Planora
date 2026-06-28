package com.yusufteker.pulse.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for the MVI architecture pattern.
 *
 * Provides a structured approach to managing:
 * - [state]: Observable UI state via [StateFlow]
 * - [effect]: One-time side effects via [Channel] → [Flow]
 * - [onEvent]: Single entry point for processing UI events
 *
 * @param S The screen's state type implementing [UiState]
 * @param E The screen's event type implementing [UiEvent]
 * @param F The screen's effect type implementing [UiEffect]
 * @param initialState The initial state of the screen
 */
import com.yusufteker.pulse.core.snackbar.SnackbarManager
import com.yusufteker.pulse.core.snackbar.SnackbarType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) : ViewModel(), KoinComponent {

    private val snackbarManager: SnackbarManager by inject()

    /**
     * Uygulama genelinde uyarı mesajı göstermek için kullanılır.
     */
    protected fun showSnackbar(message: String, type: SnackbarType = SnackbarType.INFO) {
        snackbarManager.showMessage(message, type)
    }

    private val _state = MutableStateFlow(initialState)

    /**
     * The current UI state, exposed as a [StateFlow].
     * Collectors will receive the latest state and all subsequent updates.
     */
    val state: StateFlow<S> = _state.asStateFlow()

    /**
     * The current state value. Use this inside [onEvent] or [setState]
     * to read the current state synchronously.
     */
    protected val currentState: S get() = _state.value

    private val _effect = Channel<F>(Channel.BUFFERED)

    /**
     * One-time side effects exposed as a [Flow].
     * Each effect is delivered exactly once and is not replayed.
     */
    val effect: Flow<F> = _effect.receiveAsFlow()

    /**
     * Process a UI event. This is the single entry point for all
     * user interactions and system triggers.
     *
     * Implementations should handle each event type and call
     * [setState] or [setEffect] accordingly.
     */
    abstract fun onEvent(event: E)

    /**
     * Update the UI state using a reducer function.
     * The reducer receives the current state and returns the new state.
     *
     * This is thread-safe and can be called from any coroutine.
     *
     * ```kotlin
     * setState { copy(isLoading = true) }
     * ```
     */
    protected fun setState(reduce: S.() -> S) {
        _state.update(reduce)
    }

    /**
     * Emit a one-time side effect.
     * Effects are buffered and delivered in order.
     *
     * ```kotlin
     * setEffect(HomeEffect.NavigateToProfile)
     * ```
     */
    protected fun setEffect(effect: F) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    /**
     * Launch a coroutine in the [viewModelScope].
     * Convenience wrapper for common async operations.
     */
    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
