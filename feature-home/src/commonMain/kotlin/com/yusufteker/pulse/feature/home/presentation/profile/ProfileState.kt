package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.UiState

data class ProfileState(
    val name: String = "Kullanıcı Adı",
    val avatarId: String = "avatar_1",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false
) : UiState
