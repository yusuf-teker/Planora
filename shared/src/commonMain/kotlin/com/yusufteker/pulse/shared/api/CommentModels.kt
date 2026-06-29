package com.yusufteker.pulse.shared.api

import kotlinx.serialization.Serializable

/**
 * Backend ile İstemci (Android/iOS) arasında yorum verisini taşımak için kullanılan Data Transfer Object (DTO).
 * @Serializable anotasyonu ile JSON'a veya JSON'dan kolayca dönüştürülmesini sağlar.
 */
@Serializable
data class CommentResponse(
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
    // Yanıtları kendi içinde tutarak "Nested Comments" (İç içe yorum) hiyerarşisi oluşturur.
    val replies: List<CommentResponse> = emptyList()
)

/**
 * Kullanıcı yeni bir yorum atarken (POST isteği atarken) Backend'e gönderilecek JSON modeli.
 */
@Serializable
data class CreateCommentRequest(
    val content: String,
    val parentCommentId: String? = null // Eğer bir yoruma yanıt atılıyorsa bu alan doldurulur.
)
