package com.yusufteker.pulse.feature.auth.di

import com.yusufteker.pulse.feature.auth.presentation.login.LoginViewModel
import com.yusufteker.pulse.feature.auth.presentation.onboarding.OnboardingViewModel
import com.yusufteker.pulse.feature.auth.presentation.register.RegisterViewModel
import com.yusufteker.pulse.feature.auth.presentation.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the Auth feature.
 *
 * Provides all ViewModels for auth-related screens.
 * Repository and use case bindings will be added in future phases.
 */
val authModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}
