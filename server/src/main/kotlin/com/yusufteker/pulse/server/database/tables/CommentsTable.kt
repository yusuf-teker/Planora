package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

/**
 * Ktor ve Exposed ORM (Object Relational Mapping) kullanarak veritabanındaki "comments" tablosunu temsil eder.
 * SQL yazmak yerine Kotlin nesneleriyle veritabanı tablolarını oluşturmamızı sağlar.
 */
object CommentsTable : UUIDTable("comments") {
    // Yorumun hangi gönderiye ait olduğunu belirten yabancı anahtar (Foreign Key)
    val postId = reference("post_id", PostsTable)
    
    // Yorumu kimin yazdığını belirten yazar ID'si
    val authorId = reference("author_id", UsersTable)
    
    // Yorumun başka bir yoruma yanıt olup olmadığını belirtir.
    // Eğer null ise bu ana bir yorumdur. Null değilse bir yanıttır (Nested Comment).
    val parentCommentId = reference("parent_comment_id", CommentsTable).nullable()
    
    // Yorumun içeriği (metin uzun olabileceği için text kullanılır)
    val content = text("content")
    
    // Yorumun atılma zamanı
    val createdAt = timestamp("created_at")
    
    // Yorumun aldığı beğeni sayısı (varsayılan 0)
    val likesCount = integer("likes_count").default(0)
}

/**
 * Tablodaki her bir satırı (Row) temsil eden Veri Varlığı (Entity) sınıfı.
 * Veritabanından okunan bir satır, otomatik olarak bu sınıfa dönüştürülür.
 */
class CommentEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CommentEntity>(CommentsTable)
    
    // "reference" olarak tanımladığımız alanları doğrudan diğer Entity'lere bağlıyoruz.
    // Böylece comment.post diyerek doğrudan gönderi nesnesine erişebiliriz.
    var post by PostEntity referencedOn CommentsTable.postId
    var author by UserEntity referencedOn CommentsTable.authorId
    
    // Yanıtlar için opsiyonel referans (Çünkü parentCommentId null olabilir)
    var parentComment by CommentEntity optionalReferencedOn CommentsTable.parentCommentId
    
    var content by CommentsTable.content
    var createdAt by CommentsTable.createdAt
    var likesCount by CommentsTable.likesCount
}
