package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.UiEvent

sealed interface ProfileEvent : UiEvent {
    data object BackClicked : ProfileEvent
    data object EditProfileClicked : ProfileEvent
    data class NameChanged(val name: String) : ProfileEvent
    data class AvatarSelected(val avatarId: String) : ProfileEvent
    data object SaveClicked : ProfileEvent
    data class LoadProfile(val userId: Int? = null) : ProfileEvent
    data object ToggleFollowClicked : ProfileEvent
}
