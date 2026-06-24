package com.yusufteker.pulse.core.base

/**
 * Marker interface for all UI states in the MVI pattern.
 *
 * Each screen defines its own data class implementing this interface
 * to represent the complete state of the screen.
 *
 * States must be immutable (data class with val properties).
 */
interface UiState
