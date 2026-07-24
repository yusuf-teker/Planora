package com.yusufteker.planora.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Handles incoming deep links globally across platforms.
 */
object DeepLinkManager {
    private val _deepLinkFlow = MutableSharedFlow<String>(extraBufferCapacity = 1, replay = 1)
    val deepLinkFlow = _deepLinkFlow.asSharedFlow()

    fun emitLink(link: String) {
        _deepLinkFlow.tryEmit(link)
    }
    
    fun consumeLink() {
        // Clear the replay buffer by emitting an empty string or resetting
        // Actually, since replay=1, we can just use a normal MutableStateFlow or handle consumption carefully.
        // Let's just emit an empty string to consume it.
        _deepLinkFlow.tryEmit("")
    }
}
