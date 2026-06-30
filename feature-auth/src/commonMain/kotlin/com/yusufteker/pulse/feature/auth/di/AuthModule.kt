package com.yusufteker.pulse.feature.auth.di

import com.yusufteker.pulse.feature.auth.data.repository.AuthRepositoryImpl
import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository
import com.yusufteker.pulse.feature.auth.domain.usecase.*
import com.yusufteker.pulse.feature.auth.presentation.login.LoginViewModel
import com.yusufteker.pulse.feature.auth.presentation.onboarding.OnboardingViewModel
import com.yusufteker.pulse.feature.auth.presentation.register.RegisterViewModel
import com.yusufteker.pulse.feature.auth.presentation.splash.SplashViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the Auth feature.
 *
 * Provides all ViewModels, Use Cases, and Repositories for auth-related screens.
 */
val authModule = module {
    // Data & Domain
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    singleOf(::LoginUseCase)
    singleOf(::RegisterUseCase)
    singleOf(::AutoLoginUseCase)
    singleOf(::LogoutUseCase)

    // Presentation
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}
