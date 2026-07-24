package com.yusufteker.planora.feature.home.presentation.social

import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.feature.home.domain.model.Comment
import com.yusufteker.planora.feature.home.domain.model.Post

/**
 * UI Event (Arayüz Etkinliği): Kullanıcının ekranda yaptığı eylemleri (Tıklama, Kaydırma vs.) temsil eder.
 */
sealed interface SocialEvent : UiEvent {
    data class OnTopicSelected(val topic: String?) : SocialEvent
    data class OnPostClicked(val post: Post) : SocialEvent // Gönderiye tıklandığında (Yorumları aç)
    data object OnCloseComments : SocialEvent // BottomSheet kapatıldığında
    data class OnReplyClicked(val comment: Comment) : SocialEvent // "Yanıtla" butonuna basıldığında
    data class OnSubmitComment(val content: String) : SocialEvent // "Gönder" ikonuna basıldığında
    data class OnBookmarkClicked(val postId: String) : SocialEvent // Bookmark tıklandığında
}
