package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.preferences.SessionPreferences
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.combine

class ProfileViewModel(
    private val sessionPreferences: SessionPreferences
) : BaseViewModel<ProfileState, ProfileEvent, ProfileEffect>(
    initialState = ProfileState()
) {

    init {
        // Flow olarak dinle: giriş/çıkış yapılınca DataStore değişir,
        // combine her iki değeri birlikte yakalar ve UI otomatik güncellenir.
        launch {
            combine(
                sessionPreferences.userNameFlow,
                sessionPreferences.userAvatarFlow
            ) { name, avatarId -> Pair(name, avatarId) }
            .collect { (name, avatarId) ->
                Napier.d(tag = "Screen", message = { "DataStore Flow geldi → isim='$name', avatar='$avatarId'" })
                setState {
                    copy(
                        name = name ?: "Kullanıcı",
                        avatarId = avatarId ?: "avatar_1"
                    )
                }
            }
        }
    }

    override fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.BackClicked -> {
                setEffect(ProfileEffect.NavigateBack)
            }

            is ProfileEvent.EditProfileClicked -> {
                setState { copy(isEditing = true) }
            }

            is ProfileEvent.NameChanged -> {
                setState { copy(name = event.name) }
            }

            is ProfileEvent.AvatarSelected -> {
                setState { copy(avatarId = event.avatarId) }
            }

            is ProfileEvent.SaveClicked -> {
                // TODO: Backend API çağrısı eklenecek (PUT /auth/profile)
                launch {
                    sessionPreferences.saveUserProfile(state.value.name, state.value.avatarId)
                }
                setState { copy(isEditing = false) }
            }
        }
    }
}
