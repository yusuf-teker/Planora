package com.yusufteker.pulse.feature.home.domain.model

/**
 * Clean Architecture'ın Domain (Çekirdek İş Mantığı) katmanındaki Yorum modelimiz.
 * UI katmanı (ViewModel ve Compose) Backend'den gelen DTO (CommentResponse) sınıflarını bilmez, 
 * sadece bu temiz Comment sınıfıyla çalışır.
 */
data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarId: String?,
    val parentCommentId: String?,
    val content: String,
    val createdAt: Long,
    val likesCount: Int,
    val replies: List<Comment> = emptyList(), // İç içe hiyerarşi
    
    // --- UI/İstemciye Özel Alanlar (Backend'den gelmez) ---
    // Optimistic Update (İyimser Güncelleme): Kullanıcı bir yorum gönderdiğinde, 
    // sunucudan başarı cevabı beklemeden yorumu arayüze (isSending = true) ile ekleriz.
    // Başarılı olursa isSending = false olur, başarısız olursa error = "Failed" olur.
    val isSending: Boolean = false, 
    val error: String? = null
)
