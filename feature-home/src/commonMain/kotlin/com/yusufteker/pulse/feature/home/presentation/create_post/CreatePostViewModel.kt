package com.yusufteker.pulse.feature.home.presentation.create_post

import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.base.UiEffect
import com.yusufteker.pulse.core.base.UiEvent
import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.feature.home.domain.repository.PostRepository
import kotlinx.coroutines.launch

data class CreatePostState(
    val content: String = "",
    val isSaving: Boolean = false,
    val showMarkdownPreview: Boolean = false
) : UiState

sealed interface CreatePostEvent : UiEvent {
    data class OnContentChanged(val content: String) : CreatePostEvent
    data object OnTogglePreview : CreatePostEvent
    data object OnSaveDraft : CreatePostEvent
    data object OnPost : CreatePostEvent
}

sealed interface CreatePostEffect : UiEffect {
    data object NavigateBack : CreatePostEffect
    data class ShowError(val message: String) : CreatePostEffect
}

class CreatePostViewModel(
    private val postId: String?,
    private val postRepository: PostRepository
) : BaseViewModel<CreatePostState, CreatePostEvent, CreatePostEffect>(CreatePostState()) {

    init {
        if (postId != null) {
            // Eğer bir gönderiyi düzenliyorsak, veritabanından yükleyelim
            viewModelScope.launch {
                val existingPost = postRepository.getPendingPostById(postId)
                if (existingPost != null) {
                    setState { copy(content = existingPost.content) }
                } else {
                    showSnackbar("Gönderi bulunamadı", com.yusufteker.pulse.core.snackbar.SnackbarType.ERROR)
                    setEffect(CreatePostEffect.NavigateBack)
                }
            }
        }
    }

    override fun onEvent(event: CreatePostEvent) {
        when (event) {
            is CreatePostEvent.OnContentChanged -> {
                setState { copy(content = event.content) }
            }
            CreatePostEvent.OnSaveDraft -> savePost(isDraft = true)
            CreatePostEvent.OnPost -> savePost(isDraft = false)
            CreatePostEvent.OnTogglePreview -> {
                setState { copy(showMarkdownPreview = !showMarkdownPreview) }
            }
        }
    }

    private fun savePost(isDraft: Boolean) {
        val content = currentState.content
        if (content.isBlank()) {
            showSnackbar("Gönderi içeriği boş olamaz", com.yusufteker.pulse.core.snackbar.SnackbarType.ERROR)
            return
        }

        viewModelScope.launch {
            setState { copy(isSaving = true) }
            try {
                if (postId != null) {
                    // Var olanı güncelliyoruz
                    postRepository.updatePendingPost(id = postId, content = content, isDraft = isDraft)
                    showSnackbar("Gönderi güncellendi", com.yusufteker.pulse.core.snackbar.SnackbarType.SUCCESS)
                } else {
                    // Yeni oluşturuyoruz
                    postRepository.createPost(content = content, isDraft = isDraft)
                    showSnackbar("Gönderi oluşturuldu", com.yusufteker.pulse.core.snackbar.SnackbarType.SUCCESS)
                }
                
                // Başarılı olursa önceki ekrana dön
                setEffect(CreatePostEffect.NavigateBack)
            } catch (e: Throwable) {
                showSnackbar(e.message ?: "Beklenmeyen bir hata oluştu", com.yusufteker.pulse.core.snackbar.SnackbarType.ERROR)
            } finally {
                setState { copy(isSaving = false) }
            }
        }
    }
}
