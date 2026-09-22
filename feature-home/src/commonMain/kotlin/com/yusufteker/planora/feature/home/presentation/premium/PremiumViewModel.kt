package com.yusufteker.planora.feature.home.presentation.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.billing.PaymentMethod
import com.yusufteker.planora.shared.getPlatformName
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Planora Premium Paywall ViewModel.
 *
 * Abonelik planlarının seçimi, platforma göre (iOS Apple Pay / Android Google Play)
 * ödeme seçeneklerinin sunulması ve gerçek veritabanı (Neon PostgreSQL) tabanlı
 * Premium durumunun senkronizasyonunu yönetir.
 *
 * @param sessionPreferences Kullanıcı oturumu ve Premium durumunu yöneten tercih deposu.
 * @param profileRepository Kullanıcı profilini ve gerçek Premium durumunu sunucudan tazeleyen depo.
 */
class PremiumViewModel(
    private val sessionPreferences: SessionPreferences,
    private val profileRepository: ProfileRepository? = null
) : ViewModel() {

    private val isIos = getPlatformName().lowercase().contains("ios")

    private val _state = MutableStateFlow(
        PremiumState(
            isIosPlatform = isIos,
            selectedPaymentMethod = if (isIos) PaymentMethod.APPLE_PAY else PaymentMethod.GOOGLE_PLAY
        )
    )
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PremiumEffect>()
    val effect = _effect.asSharedFlow()

    internal var syncJob: Job? = null

    init {
        // Kullanıcının mevcut yerel DataStore Premium durumunu reaktif dinle
        viewModelScope.launch {
            sessionPreferences.isPremiumFlow.collect { isPrem ->
                _state.update { it.copy(isPremium = isPrem) }
            }
        }

        // Ekran açıldığında Neon veritabanındaki en güncel durumu sorgula
        refreshPremiumStatus()
    }

    /**
     * UI olaylarını işleyen fonksiyon.
     */
    fun onEvent(event: PremiumEvent) {
        when (event) {
            is PremiumEvent.OnPlanSelected -> {
                _state.update { it.copy(selectedPlan = event.plan) }
            }
            is PremiumEvent.OnPaymentMethodSelected -> {
                _state.update { it.copy(selectedPaymentMethod = event.method) }
            }
            is PremiumEvent.OnStartPurchaseClicked -> {
                _state.update { it.copy(showPaymentSheet = true, errorMessage = null) }
            }
            is PremiumEvent.OnDismissPaymentSheet -> {
                _state.update { it.copy(showPaymentSheet = false) }
            }
            is PremiumEvent.OnRefreshPremiumStatus -> {
                refreshPremiumStatus()
            }
            is PremiumEvent.OnBackClicked -> {
                viewModelScope.launch {
                    _effect.emit(PremiumEffect.NavigateBack)
                }
            }
        }
    }

    /**
     * Sunucudan kullanıcının en güncel Premium durumunu sorgular ve yerel depoya kaydeder.
     */
    fun refreshPremiumStatus() {
        if (profileRepository == null) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                val result = profileRepository.getProfile("me")
                result.onSuccess { profile ->
                    sessionPreferences.setPremium(profile.isPremium, profile.premiumUntil)
                    _state.update {
                        it.copy(
                            isPremium = profile.isPremium,
                            isSyncing = false
                        )
                    }
                }.onFailure {
                    _state.update { it.copy(isSyncing = false) }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(isSyncing = false) }
            }
        }
    }
}
