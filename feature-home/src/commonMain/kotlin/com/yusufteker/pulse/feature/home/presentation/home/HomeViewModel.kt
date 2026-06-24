package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.BaseViewModel

/**
 * ViewModel for the Home screen.
 *
 * Manages home feed state and navigation.
 * Feed loading will be added in future phases.
 */
class HomeViewModel : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState()
) {

    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ProfileClicked -> {
                setEffect(HomeEffect.NavigateToProfile)
            }

            is HomeEvent.SettingsClicked -> {
                setEffect(HomeEffect.NavigateToSettings)
            }

            is HomeEvent.RefreshRequested -> {
                // TODO: Refresh feed from repository
                setState { copy(isLoading = true) }
                launch {
                    // Simulate loading
                    kotlinx.coroutines.delay(1000)
                    setState { copy(isLoading = false) }
                }
            }
        }
    }
}
