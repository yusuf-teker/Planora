package com.yusufteker.pulse.feature.home.presentation.focus

sealed interface FocusEffect {
    data object NavigateBack : FocusEffect
    data class ShowSnackbar(val message: String) : FocusEffect
}
