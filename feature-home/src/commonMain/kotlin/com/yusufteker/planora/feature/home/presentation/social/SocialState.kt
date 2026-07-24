package com.yusufteker.planora.feature.home.presentation.social

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.feature.home.domain.model.Comment
import com.yusufteker.planora.feature.home.domain.model.Post

/**
 * UI State (Arayüz Durumu): Ekrandaki tüm verileri temsil eden durum (State) sınıfı.
 * StateFlow ile dinlendiği için buradaki her değişiklik ekranda anında güncellenir (Recomposition).
 */
data class SocialState(
    val isLoading: Boolean = false, 
    val isLoggedIn: Boolean = false, // Misafir modu kontrolü
    val selectedTopic: String? = null,
    val selectedPostForComments: Post? = null, // Yorumlar için tıklanan gönderi
    val comments: List<Comment> = emptyList(), // O gönderiye ait yorum listesi
    val isCommentsLoading: Boolean = false,
    val replyToComment: Comment? = null // Eğer bir yoruma "Yanıtla" denildiyse o yorum
) : UiState
