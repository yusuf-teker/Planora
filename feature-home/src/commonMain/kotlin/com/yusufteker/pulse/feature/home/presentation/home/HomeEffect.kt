package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.UiEffect

/**
 * Side effects for the Home screen.
 */
sealed interface HomeEffect : UiEffect {
    data object NavigateToProfile : HomeEffect
    data object NavigateToSettings : HomeEffect
}
