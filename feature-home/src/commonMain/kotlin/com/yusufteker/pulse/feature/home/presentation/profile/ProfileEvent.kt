package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.UiEvent

sealed interface ProfileEvent : UiEvent {
    data object BackClicked : ProfileEvent
    data object EditProfileClicked : ProfileEvent
}
