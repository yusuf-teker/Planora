package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.UiEffect

sealed interface ProfileEffect : UiEffect {
    data object NavigateBack : ProfileEffect
}
