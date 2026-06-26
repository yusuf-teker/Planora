package com.yusufteker.pulse.feature.home.di

import com.yusufteker.pulse.feature.home.presentation.home.HomeViewModel
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsViewModel
import com.yusufteker.pulse.feature.home.presentation.social.SocialViewModel
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import com.yusufteker.pulse.feature.home.data.repository.ProfileRepositoryImpl
import com.yusufteker.pulse.feature.home.data.api.FeedApi
import com.yusufteker.pulse.feature.home.domain.repository.FeedRepository
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the Home feature.
 */
val homeModule = module {
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    single { FeedApi(get()) }
    single { FeedRepository(get(), get()) }
    
    viewModelOf(::HomeViewModel)
    viewModelOf(::SocialViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
}
