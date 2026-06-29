package com.yusufteker.pulse.feature.home.domain.repository

import com.yusufteker.pulse.feature.home.data.api.CommentApi
import com.yusufteker.pulse.feature.home.domain.model.Comment
import com.yusufteker.pulse.shared.api.CommentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Repository (Depo) Sınıfı: Clean Architecture'ın bir parçasıdır.
 * API'den (veya veritabanından) gelen veriyi (DTO'ları) uygulamanın anlayacağı
 * saf Domain Model'lerine (Comment) çevirme işini yapar.
 */
class CommentRepository(private val api: CommentApi) {

    // Yorumları API'den çeker ve DTO -> Domain Model dönüşümü yapar.
    suspend fun getComments(postId: String): List<Comment> {
        return withContext(Dispatchers.IO) {
            val responses = api.getComments(postId)
            responses.map { it.toDomainModel() } // Mapper çağrısı
        }
    }

    // Yeni yorum atma işlemini yönetir.
    suspend fun addComment(postId: String, content: String, parentCommentId: String?): Result<Comment> {
        return withContext(Dispatchers.IO) {
            api.createComment(postId, content, parentCommentId).map { it.toDomainModel() }
        }
    }

    /**
     * DTO nesnesini (CommentResponse) alıp, UI tarafında kullanılacak 
     * Comment nesnesine çeviren Yardımcı Fonksiyon (Mapper).
     */
    private fun CommentResponse.toDomainModel(): Comment {
        return Comment(
            id = id,
            postId = postId,
            authorId = authorId,
            authorName = authorName,
            authorUsername = authorUsername,
            authorAvatarId = authorAvatarId,
            parentCommentId = parentCommentId,
            content = content,
            createdAt = createdAt,
            likesCount = likesCount,
            replies = replies.map { it.toDomainModel() }
        )
    }
}
