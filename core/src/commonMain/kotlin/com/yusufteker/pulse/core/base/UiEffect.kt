package com.yusufteker.pulse.core.base

/**
 * Marker interface for all UI side effects in the MVI pattern.
 *
 * Effects represent one-time events that should not be replayed
 * on configuration changes (e.g., navigation, snackbar, toast).
 *
 * Effects are emitted via a Channel and consumed as a Flow.
 */
interface UiEffect
