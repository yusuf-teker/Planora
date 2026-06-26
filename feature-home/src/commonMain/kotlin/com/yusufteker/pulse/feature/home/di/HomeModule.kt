package com.yusufteker.pulse.feature.home.di

import com.yusufteker.pulse.feature.home.presentation.home.HomeViewModel
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsViewModel
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import com.yusufteker.pulse.feature.home.data.repository.ProfileRepositoryImpl
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the Home feature.
 */
val homeModule = module {
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    
    viewModelOf(::HomeViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
}
