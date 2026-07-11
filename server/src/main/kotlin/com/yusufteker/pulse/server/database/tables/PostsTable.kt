package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object PostsTable : UUIDTable("posts") {
    val authorId = reference("author_id", UsersTable).index()
    val content = text("content")
    val createdAt = timestamp("created_at").index()
    val likesCount = integer("likes_count").default(0)
    val commentsCount = integer("comments_count").default(0)
    val topic = varchar("topic", 50).default("GENERAL").index()
}

class PostEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PostEntity>(PostsTable)
    
    var author by UserEntity referencedOn PostsTable.authorId
    var content by PostsTable.content
    var createdAt by PostsTable.createdAt
    var likesCount by PostsTable.likesCount
    var commentsCount by PostsTable.commentsCount
    var topic by PostsTable.topic
}
