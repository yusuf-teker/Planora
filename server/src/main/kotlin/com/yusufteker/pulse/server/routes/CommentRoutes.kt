package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.CommentEntity
import com.yusufteker.pulse.server.database.tables.CommentsTable
import com.yusufteker.pulse.server.database.tables.PostEntity
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.shared.api.CommentResponse
import com.yusufteker.pulse.shared.api.CreateCommentRequest
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.request.receiveNullable
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.SortOrder
import java.util.UUID

fun Route.commentRoutes() {
    // "auth-jwt" ile bu rotadaki işlemlerin sadece giriş yapmış kullanıcılar tarafından erişilmesini sağlıyoruz.
    authenticate("auth-jwt") {
        
        // Ortak rota yolunu tanımlar. Örneğin: GET /posts/123/comments
        route("/posts/{postId}/comments") {
            
            // Yorumları Listeleme Endpoint'i
            get {
                // URL'den postId değerini güvenli bir şekilde alıp UUID formatına çeviriyoruz.
                val postIdString = call.parameters["postId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing postId")
                val postId = try {
                    UUID.fromString(postIdString)
                } catch (e: Exception) {
                    return@get call.respond(HttpStatusCode.BadRequest, "Invalid postId")
                }

                // Veritabanı sorgularını dbQuery bloğu (Coroutine + Transaction) içinde çalıştırıyoruz.
                val comments = dbQuery {
                    // 1. Gönderiye ait tüm yorumları eskiden yeniye doğru sıralı şekilde getir.
                    val allComments = CommentEntity.find { CommentsTable.postId eq postId }
                        .orderBy(CommentsTable.createdAt to SortOrder.ASC)
                        .toList()
                        
                    // Nested (İç içe) yorum hiyerarşisi oluşturmak için haritalar (Map) kullanacağız.
                    val commentMap = mutableMapOf<UUID, CommentResponse>()
                    val rootComments = mutableListOf<CommentResponse>()
                    
                    // First pass (İlk Aşama): Tüm yorumları düz (flat) bir liste halinde DTO (Veri Transfer Objesi) modeline çevir
                    // Bu aşamada henüz yanıtları (replies) doldurmuyoruz.
                    allComments.forEach { entity ->
                        val author = entity.author
                        val dto = CommentResponse(
                            id = entity.id.value.toString(),
                            postId = entity.post.id.value.toString(),
                            authorId = author.id.value.toString(),
                            authorName = author.name,
                            authorUsername = author.name.lowercase().replace(" ", "_"),
                            authorAvatarId = author.avatarId,
                            parentCommentId = entity.parentComment?.id?.value?.toString(),
                            content = entity.content,
                            createdAt = entity.createdAt.toEpochMilli(),
                            likesCount = entity.likesCount,
                            replies = emptyList() // Yanıtları ikinci aşamada bağlayacağız
                        )
                        commentMap[entity.id.value] = dto
                    }
                    
                    // Second pass (İkinci Aşama): Hangi yorumun hangi ana yoruma yanıt olduğunu grupla
                    val replyMap = mutableMapOf<String, MutableList<CommentResponse>>()
                    allComments.forEach { entity ->
                        val parentId = entity.parentComment?.id?.value?.toString()
                        if (parentId != null) {
                            val list = replyMap.getOrPut(parentId) { mutableListOf() }
                            list.add(commentMap[entity.id.value]!!)
                        }
                    }
                    
                    // Üçüncü Aşama: Yorumları kendi yanıtlarıyla (replies) birleştir.
                    allComments.forEach { entity ->
                        val parentId = entity.parentComment?.id?.value?.toString()
                        val currentIdStr = entity.id.value.toString()
                        
                        // Yorumun yanıtlarını replyMap'ten çekip içine koyuyoruz.
                        val dto = commentMap[entity.id.value]!!.copy(replies = replyMap[currentIdStr] ?: emptyList())
                        commentMap[entity.id.value] = dto
                        
                        // Sadece en üst seviye (parent'ı olmayan) yorumları ana listeye ekle.
                        // Diğerleri zaten üst seviye yorumların replies dizisi içine girdi.
                        if (parentId == null) {
                            rootComments.add(dto)
                        }
                    }
                    
                    rootComments
                }

                // HTTP 200 OK ile JSON listesini istemciye gönder.
                call.respond(HttpStatusCode.OK, comments)
            }

            // Yeni Yorum Ekleme Endpoint'i (POST)
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }
                
                val postIdString = call.parameters["postId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing postId")
                val postId = try {
                    UUID.fromString(postIdString)
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid postId")
                }

                val request = call.receiveNullable<CreateCommentRequest>()
                if (request == null || request.content.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Content cannot be empty")
                    return@post
                }
                
                val parentId = request.parentCommentId?.let { 
                    try { UUID.fromString(it) } catch(e: Exception) { null } 
                }

                val newCommentDto = dbQuery {
                    val user = UserEntity.findById(userId) ?: return@dbQuery null
                    val post = PostEntity.findById(postId) ?: return@dbQuery null
                    val parentComment = parentId?.let { CommentEntity.findById(it) }

                    // Yeni bir kayıt oluştur (INSERT işlemi)
                    val newComment = CommentEntity.new {
                        this.author = user
                        this.post = post
                        this.parentComment = parentComment
                        this.content = request.content
                        this.createdAt = java.time.Instant.now()
                        this.likesCount = 0
                    }
                    
                    // Gönderinin yorum sayısını 1 artır. Exposed bunu otomatik olarak "UPDATE posts" sorgusuna çevirir.
                    post.commentsCount += 1
                    
                    // Yeni oluşturduğumuz yorumu istemciye dönmek için DTO'ya çeviriyoruz.
                    com.yusufteker.pulse.shared.api.CommentResponse(
                        id = newComment.id.value.toString(),
                        postId = newComment.post.id.value.toString(),
                        authorId = user.id.value.toString(),
                        authorName = user.name,
                        authorUsername = user.name.lowercase().replace(" ", "_"),
                        authorAvatarId = user.avatarId,
                        parentCommentId = newComment.parentComment?.id?.value?.toString(),
                        content = newComment.content,
                        createdAt = newComment.createdAt.toEpochMilli(),
                        likesCount = newComment.likesCount,
                        replies = emptyList<com.yusufteker.pulse.shared.api.CommentResponse>()
                    )
                }
                
                if (newCommentDto == null) {
                    call.respond(HttpStatusCode.NotFound, "Post or User not found")
                } else {
                    call.respond(HttpStatusCode.Created, newCommentDto)
                }
            }
        }
    }
}
