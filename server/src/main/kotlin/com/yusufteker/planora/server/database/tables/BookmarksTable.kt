package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object BookmarksTable : IntIdTable("bookmarks") {
    val userId = reference("user_id", UsersTable)
    val postId = reference("post_id", PostsTable)
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(userId, postId)
    }
}

class BookmarkEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BookmarkEntity>(BookmarksTable)

    var user by UserEntity referencedOn BookmarksTable.userId
    var post by PostEntity referencedOn BookmarksTable.postId
    var createdAt by BookmarksTable.createdAt
}
