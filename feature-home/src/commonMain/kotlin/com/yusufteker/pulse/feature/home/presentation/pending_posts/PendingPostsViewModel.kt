package com.yusufteker.pulse.feature.home.presentation.pending_posts

import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.base.UiEffect
import com.yusufteker.pulse.core.base.UiEvent
import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.database.PendingPostEntity
import com.yusufteker.pulse.feature.home.domain.repository.PostRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch



class PendingPostsViewModel(
    private val postRepository: PostRepository
) : BaseViewModel<PendingPostsState, PendingPostsEvent, PendingPostsEffect>(PendingPostsState()) {

    init {
        // Veritabanındaki değişiklikleri canlı olarak dinliyoruz
        postRepository.getAllPendingPosts()
            .onEach { posts ->
                setState { copy(posts = posts, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: PendingPostsEvent) {
        when (event) {
            is PendingPostsEvent.OnPostClicked -> {
                setEffect(PendingPostsEffect.NavigateToEditPost(event.postId))
            }
            is PendingPostsEvent.OnDeleteClicked -> {
                viewModelScope.launch {
                    postRepository.deletePendingPost(event.postId)
                }
            }
        }
    }
}
