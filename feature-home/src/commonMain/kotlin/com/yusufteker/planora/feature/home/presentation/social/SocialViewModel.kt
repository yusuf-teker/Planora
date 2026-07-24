package com.yusufteker.planora.feature.home.presentation.social

import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.domain.model.Post
import com.yusufteker.planora.feature.home.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.base.UiEffect
import com.yusufteker.planora.feature.home.domain.model.Comment
import com.yusufteker.planora.feature.home.domain.repository.CommentRepository
import kotlinx.coroutines.launch

import kotlin.random.Random



class SocialViewModel(
    private val feedRepository: FeedRepository,
    private val commentRepository: CommentRepository,
    private val sessionPreferences: SessionPreferences
) : BaseViewModel<SocialState, SocialEvent, SocialEffect>(
    initialState = SocialState()
) {
    init {
        sessionPreferences.userIdFlow.onEach { userId ->
            setState { copy(isLoggedIn = userId != null) }
        }.launchIn(viewModelScope)
    }

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
            is SocialEvent.OnPostClicked -> {
                setState { copy(selectedPostForComments = event.post, isCommentsLoading = true, comments = emptyList()) }
                fetchComments(event.post.id)
            }
            SocialEvent.OnCloseComments -> {
                setState { copy(selectedPostForComments = null, comments = emptyList(), replyToComment = null) }
            }
            is SocialEvent.OnReplyClicked -> {
                setState { copy(replyToComment = event.comment) }
            }
            is SocialEvent.OnSubmitComment -> {
                val postId = state.value.selectedPostForComments?.id ?: return
                val parentId = state.value.replyToComment?.id
                
                // === OPTIMISTIC UPDATE (İyimser Güncelleme) BAŞLANGICI ===
                // Kullanıcı yorumu gönderir göndermez, sunucunun cevabını beklemeden,
                // geçici bir id ile oluşturup arayüze ekliyoruz. Böylece anında tepki veriyoruz.
                val tempId = "temp_${kotlin.random.Random.nextInt()}"
                val tempComment = Comment(
                    id = tempId,
                    postId = postId,
                    authorId = "",
                    authorName = "You",
                    authorUsername = "you",
                    authorAvatarId = null,
                    parentCommentId = parentId,
                    content = event.content,
                    createdAt = Random.nextLong(1000000L, 9000000L),
                    likesCount = 0,
                    isSending = true // Şeffaf/soluk gözükmesini sağlayacak
                )
                
                // Yorumu State'e Ekle
                if (parentId == null) {
                    setState { copy(comments = comments + tempComment, replyToComment = null) }
                } else {
                    setState { copy(
                        comments = comments.map { c ->
                            if (c.id == parentId) c.copy(replies = c.replies + tempComment)
                            else c
                        },
                        replyToComment = null
                    )}
                }
                // === OPTIMISTIC UPDATE BİTİŞİ ===
                
                // Artık gerçek isteği API'ye atıyoruz.
                viewModelScope.launch {
                    val result = commentRepository.addComment(postId, event.content, parentId)
                    result.onSuccess { realComment ->
                        // Başarılı olursa, oluşturduğumuz geçici (temp) yorumu gerçek yorumla değiştir
                        if (parentId == null) { // Ana yorum ise
                            setState { copy(comments = comments.map { if (it.id == tempId) realComment else it }) }
                        } else { // Yanıt (reply) ise
                            setState { copy(comments = comments.map { c ->
                                if (c.id == parentId) {
                                    c.copy(replies = c.replies.map { if (it.id == tempId) realComment else it })
                                } else c
                            })}
                        }
                    }.onFailure { error ->
                        // Başarısız olursa (Örn: İnternet koptu), isSending=false yap ve Hata mesajı (error="Failed") ekle
                        if (parentId == null) { // Ana yorum ise
                            setState { copy(comments = comments.map { if (it.id == tempId) it.copy(isSending = false, error = "Failed") else it }) }
                        } else { //
                            setState { copy(comments = comments.map { c ->
                                if (c.id == parentId) {
                                    c.copy(replies = c.replies.map { if (it.id == tempId) it.copy(isSending = false, error = "Failed") else it })
                                } else c
                            })}
                        }
                    }
                }
            }
            
            is SocialEvent.OnBookmarkClicked -> {
                viewModelScope.launch {
                    feedRepository.toggleBookmark(event.postId)
                }
            }
        }
    }

    private fun fetchComments(postId: String) {
        viewModelScope.launch {
            val comments = commentRepository.getComments(postId)
            setState { copy(comments = comments, isCommentsLoading = false) }
        }
    }
}
