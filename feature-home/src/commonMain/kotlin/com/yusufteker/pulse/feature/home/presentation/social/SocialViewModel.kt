package com.yusufteker.pulse.feature.home.presentation.social

import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.home.domain.model.Post
import com.yusufteker.pulse.feature.home.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.base.UiEvent
import com.yusufteker.pulse.core.base.UiEffect

data class SocialState(val isLoading: Boolean = false) : UiState
sealed interface SocialEvent : UiEvent
sealed interface SocialEffect : UiEffect

class SocialViewModel(
    feedRepository: FeedRepository
) : BaseViewModel<SocialState, SocialEvent, SocialEffect>(
    initialState = SocialState()
) {
    val feedPagingData: Flow<PagingData<Post>> = feedRepository.getFeed().cachedIn(viewModelScope)

    override fun onEvent(event: SocialEvent) {
        // Handle events
    }
}
