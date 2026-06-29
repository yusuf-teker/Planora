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
import com.yusufteker.pulse.feature.home.domain.model.Comment
import com.yusufteker.pulse.feature.home.domain.repository.CommentRepository
import kotlinx.coroutines.launch

/**
 * UI State (Arayüz Durumu): Ekrandaki tüm verileri temsil eden durum (State) sınıfı.
 * StateFlow ile dinlendiği için buradaki her değişiklik ekranda anında güncellenir (Recomposition).
 */
data class SocialState(
    val isLoading: Boolean = false, 
    val selectedTopic: String? = null,
    val selectedPostForComments: Post? = null, // Yorumlar için tıklanan gönderi
    val comments: List<Comment> = emptyList(), // O gönderiye ait yorum listesi
    val isCommentsLoading: Boolean = false,
    val replyToComment: Comment? = null // Eğer bir yoruma "Yanıtla" denildiyse o yorum
) : UiState

/**
 * UI Event (Arayüz Etkinliği): Kullanıcının ekranda yaptığı eylemleri (Tıklama, Kaydırma vs.) temsil eder.
 */
sealed interface SocialEvent : UiEvent {
    data class OnTopicSelected(val topic: String?) : SocialEvent
    data class OnPostClicked(val post: Post) : SocialEvent // Gönderiye tıklandığında (Yorumları aç)
    data object OnCloseComments : SocialEvent // BottomSheet kapatıldığında
    data class OnReplyClicked(val comment: Comment) : SocialEvent // "Yanıtla" butonuna basıldığında
    data class OnSubmitComment(val content: String) : SocialEvent // "Gönder" ikonuna basıldığında
}
sealed interface SocialEffect : UiEffect

class SocialViewModel(
    feedRepository: FeedRepository,
    private val commentRepository: CommentRepository
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
                    createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
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
        }
    }

    private fun fetchComments(postId: String) {
        viewModelScope.launch {
            val comments = commentRepository.getComments(postId)
            setState { copy(comments = comments, isCommentsLoading = false) }
        }
    }
}
