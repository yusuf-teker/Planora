package com.yusufteker.pulse.feature.home.presentation.social

import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.home.domain.model.Post
import com.yusufteker.pulse.feature.home.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.base.UiEvent
import com.yusufteker.pulse.core.base.UiEffect

data class SocialState(val isLoading: Boolean = false, val selectedTopic: String? = null) : UiState
sealed interface SocialEvent : UiEvent {
    data class OnTopicSelected(val topic: String?) : SocialEvent
}
sealed interface SocialEffect : UiEffect

class SocialViewModel(
    feedRepository: FeedRepository
) : BaseViewModel<SocialState, SocialEvent, SocialEffect>(
    initialState = SocialState()
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val feedPagingData: Flow<PagingData<Post>> = state
        .map { it.selectedTopic }
        .distinctUntilChanged()
        .flatMapLatest { topic ->
            feedRepository.getFeed(topic)
        }
        .cachedIn(viewModelScope)

    override fun onEvent(event: SocialEvent) {
        when (event) {
            is SocialEvent.OnTopicSelected -> setState { copy(selectedTopic = event.topic) }
        }
    }
}
