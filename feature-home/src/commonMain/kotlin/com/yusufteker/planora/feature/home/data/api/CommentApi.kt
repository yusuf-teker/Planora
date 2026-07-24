package com.yusufteker.planora.feature.home.data.api

import com.yusufteker.planora.shared.api.CommentResponse
import com.yusufteker.planora.shared.api.CreateCommentRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * İstemci tarafında (Client-side) Backend ile iletişim kuran Ktor API sınıfı.
 * HTTP çağrılarını (GET, POST) yapar ve gelen JSON cevaplarını DTO sınıflarına (CommentResponse) çevirir.
 */
class CommentApi(private val httpClient: HttpClient) {

    // Belirli bir gönderiye ait yorumları çeker.
    suspend fun getComments(postId: String): List<CommentResponse> {
        return try {
            // Ktor HttpClient ile GET isteği atılır.
            httpClient.get("posts/$postId/comments").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Yeni bir yorum (veya yanıt) gönderir.
    suspend fun createComment(postId: String, content: String, parentCommentId: String?): Result<CommentResponse> {
        return try {
            // Ktor HttpClient ile POST isteği atılır. 
            // İçeriğe JSON olarak CreateCommentRequest sınıfı konur.
            val response: CommentResponse = httpClient.post("posts/$postId/comments") {
                contentType(ContentType.Application.Json)
                setBody(CreateCommentRequest(content = content, parentCommentId = parentCommentId))
            }.body()
            
            // İşlem başarılıysa Result.success döneriz.
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            // Hata olursa (örn: internet yok) Result.failure döneriz.
            Result.failure(e)
        }
    }
}
