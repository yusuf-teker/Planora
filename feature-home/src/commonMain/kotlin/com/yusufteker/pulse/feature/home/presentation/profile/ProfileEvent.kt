package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.UiEvent

sealed interface ProfileEvent : UiEvent {
    data object BackClicked : ProfileEvent
    data object EditProfileClicked : ProfileEvent
    data class NameChanged(val name: String) : ProfileEvent
    data class AvatarSelected(val avatarId: String) : ProfileEvent
    data class ProfileImageSelected(val imageBytes: ByteArray) : ProfileEvent
    data object SaveClicked : ProfileEvent
    data class LoadProfile(val userId: Int? = null) : ProfileEvent
    data object ToggleFollowClicked : ProfileEvent
    data object LogoutClicked : ProfileEvent
    data class NavigateToFollowList(val tab: Int) : ProfileEvent
    data class AcceptRequestClicked(val requestId: Int) : ProfileEvent
    data class RejectRequestClicked(val requestId: Int) : ProfileEvent
    data object RequestCalendarAccessClicked : ProfileEvent
    data object RevokeCalendarAccessClicked : ProfileEvent
    data class AcceptCalendarRequestClicked(val requestId: Int) : ProfileEvent
    data class RejectCalendarRequestClicked(val requestId: Int) : ProfileEvent
    data class RevokeCalendarGrantClicked(val userId: Int) : ProfileEvent
}
