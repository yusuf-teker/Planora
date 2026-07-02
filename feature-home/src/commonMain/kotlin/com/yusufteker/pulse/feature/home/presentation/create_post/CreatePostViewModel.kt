package com.yusufteker.pulse.feature.home.presentation.create_post

import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.base.UiEffect
import com.yusufteker.pulse.core.base.UiEvent
import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.feature.home.domain.repository.PostRepository
import kotlinx.coroutines.launch
import com.yusufteker.pulse.core.analytics.AnalyticsManager

data class CreatePostState(
    val content: String = "",
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val originalIsDraft: Boolean = false,
    val selectedTopic: String = "GENERAL"
) : UiState

sealed interface CreatePostEvent : UiEvent {
    data class OnPost(val content: String) : CreatePostEvent
    data class OnSaveDraft(val content: String) : CreatePostEvent
    data class OnTopicSelected(val topic: String) : CreatePostEvent
}

sealed interface CreatePostEffect : UiEffect {
    data object NavigateBack : CreatePostEffect
    data class ShowError(val message: String) : CreatePostEffect
}

class CreatePostViewModel(
    private val postId: String?,
    private val postRepository: PostRepository,
    private val analyticsManager: AnalyticsManager
) : BaseViewModel<CreatePostState, CreatePostEvent, CreatePostEffect>(CreatePostState()) {

    init {
        if (postId != null) {
            // Eğer bir gönderiyi düzenliyorsak, veritabanından yükleyelim
            viewModelScope.launch {
                val existingPost = postRepository.getPendingPostById(postId)
                if (existingPost != null) {
                    setState { 
                        copy(
                            content = existingPost.content, 
                            isEditing = true, 
                            originalIsDraft = existingPost.isDraft == 1L,
                            selectedTopic = existingPost.topic
                        ) 
                    }
                } else {
                    showSnackbar("Gönderi bulunamadı", com.yusufteker.pulse.core.snackbar.SnackbarType.ERROR)
                    setEffect(CreatePostEffect.NavigateBack)
                }
            }
        }
    }

    override fun onEvent(event: CreatePostEvent) {
        when (event) {
            is CreatePostEvent.OnSaveDraft -> savePost(content = event.content, isDraft = true, topic = currentState.selectedTopic)
            is CreatePostEvent.OnPost -> savePost(content = event.content, isDraft = false, topic = currentState.selectedTopic)
            is CreatePostEvent.OnTopicSelected -> setState { copy(selectedTopic = event.topic) }
        }
    }

    private fun savePost(content: String, isDraft: Boolean, topic: String) {
        if (content.isBlank()) {
            showSnackbar("Gönderi içeriği boş olamaz", com.yusufteker.pulse.core.snackbar.SnackbarType.ERROR)
            return
        }

        viewModelScope.launch {
            setState { copy(isSaving = true) }
            try {
                val originalIsDraft = currentState.originalIsDraft
                // Eğer eski post "Bekleyen" (isDraft=false) ve biz bunu "Taslak" (isDraft=true) yapmak istiyorsak,
                // bunu güncellemek yerine yeni bir taslak olarak yaratırız.
                val shouldCreateNew = (postId == null) || (isDraft && !originalIsDraft)

                if (!shouldCreateNew) {
                    // Var olanı güncelliyoruz
                    postRepository.updatePendingPost(id = postId!!, content = content, isDraft = isDraft, topic = topic)
                    val message = if (isDraft) "Taslak güncellendi" else "Gönderi güncellendi"
                    showSnackbar(message, com.yusufteker.pulse.core.snackbar.SnackbarType.SUCCESS)
                } else {
                    // Yeni oluşturuyoruz
                    postRepository.createPost(content = content, isDraft = isDraft, topic = topic)
                    
                    val eventName = if (isDraft) "draft_created" else "post_created"
                    analyticsManager.logEvent(eventName, mapOf("topic" to topic))
                    
                    val message = if (isDraft) "Taslak olarak kaydedildi" else "Gönderi oluşturuldu"
                    showSnackbar(message, com.yusufteker.pulse.core.snackbar.SnackbarType.SUCCESS)
                }
                
                // Başarılı olursa önceki ekrana dön
                setEffect(CreatePostEffect.NavigateBack)
            } catch (e: Throwable) {
                analyticsManager.logException(e)
                showSnackbar(e.message ?: "Beklenmeyen bir hata oluştu", com.yusufteker.pulse.core.snackbar.SnackbarType.ERROR)
            } finally {
                setState { copy(isSaving = false) }
            }
        }
    }
}
