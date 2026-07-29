package com.yusufteker.planora.feature.auth.di

import com.yusufteker.planora.feature.auth.data.repository.AuthRepositoryImpl
import com.yusufteker.planora.feature.auth.domain.repository.AuthRepository
import com.yusufteker.planora.feature.auth.domain.usecase.*
import com.yusufteker.planora.feature.auth.presentation.forgot_password.ForgotPasswordViewModel
import com.yusufteker.planora.feature.auth.presentation.login.LoginViewModel
import com.yusufteker.planora.feature.auth.presentation.onboarding.OnboardingViewModel
import com.yusufteker.planora.feature.auth.presentation.register.RegisterViewModel
import com.yusufteker.planora.feature.auth.presentation.splash.SplashViewModel
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
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    singleOf(::LoginUseCase)
    singleOf(::RegisterUseCase)
    singleOf(::SendRegisterCodeUseCase)
    singleOf(::AutoLoginUseCase)
    singleOf(::LogoutUseCase)

    // Presentation
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ForgotPasswordViewModel)
}


