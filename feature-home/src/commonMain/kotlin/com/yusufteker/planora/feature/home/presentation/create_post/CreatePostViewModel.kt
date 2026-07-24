package com.yusufteker.planora.feature.home.presentation.create_post

import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.base.UiEffect
import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.feature.home.domain.repository.PostRepository
import kotlinx.coroutines.launch
import com.yusufteker.planora.core.analytics.AnalyticsManager
import org.jetbrains.compose.resources.getString
import planora.core.generated.resources.Res
import planora.core.generated.resources.*



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
                    showSnackbar(getString(Res.string.error_post_not_found), com.yusufteker.planora.core.snackbar.SnackbarType.ERROR)
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
            viewModelScope.launch {
                showSnackbar(getString(Res.string.error_post_content_empty), com.yusufteker.planora.core.snackbar.SnackbarType.ERROR)
            }
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
                    postRepository.updatePendingPost(id = postId, content = content, isDraft = isDraft, topic = topic)
                    val message = if (isDraft) getString(Res.string.info_draft_updated) else getString(Res.string.info_post_updated)
                    showSnackbar(message, com.yusufteker.planora.core.snackbar.SnackbarType.SUCCESS)
                } else {
                    // Yeni oluşturuyoruz
                    postRepository.createPost(content = content, isDraft = isDraft, topic = topic)
                    
                    val eventName = if (isDraft) "draft_created" else "post_created"
                    analyticsManager.logEvent(eventName, mapOf("topic" to topic))
                    
                    val message = if (isDraft) getString(Res.string.info_draft_saved) else getString(Res.string.info_post_created)
                    showSnackbar(message, com.yusufteker.planora.core.snackbar.SnackbarType.SUCCESS)
                }
                
                // Başarılı olursa önceki ekrana dön
                setEffect(CreatePostEffect.NavigateBack)
            } catch (e: Throwable) {
                analyticsManager.logException(e)
                showSnackbar(e.message ?: getString(Res.string.error_unexpected), com.yusufteker.planora.core.snackbar.SnackbarType.ERROR)
            } finally {
                setState { copy(isSaving = false) }
            }
        }
    }
}
