package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.BaseViewModel

class ProfileViewModel : BaseViewModel<ProfileState, ProfileEvent, ProfileEffect>(
    initialState = ProfileState()
) {

    override fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.BackClicked -> {
                setEffect(ProfileEffect.NavigateBack)
            }

            is ProfileEvent.EditProfileClicked -> {
                // TODO: Navigate to edit profile
            }
        }
    }
}
