package com.yusufteker.pulse.feature.home.di

import com.yusufteker.pulse.feature.home.presentation.home.HomeViewModel
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the Home feature.
 */
val homeModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
}
