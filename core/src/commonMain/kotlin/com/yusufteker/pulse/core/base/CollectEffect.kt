package com.yusufteker.pulse.core.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects a [Flow] of side effects in a lifecycle-aware manner.
 *
 * Effects are only collected when the lifecycle is at least [STARTED],
 * preventing unnecessary processing when the UI is not visible.
 *
 * Usage:
 * ```kotlin
 * viewModel.effect.CollectEffect { effect ->
 *     when (effect) {
 *         is MyEffect.Navigate -> navigator.navigate(effect.route)
 *     }
 * }
 * ```
 */
@Composable
fun <F : UiEffect> Flow<F>.CollectEffect(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: (F) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            this@CollectEffect.collect { effect ->
                onEffect(effect)
            }
        }
    }
}
