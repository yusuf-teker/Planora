package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.PostEntity
import com.yusufteker.pulse.server.database.tables.PostsTable
import com.yusufteker.pulse.shared.api.FeedResponse
import com.yusufteker.pulse.shared.api.PostDto
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
import org.jetbrains.exposed.sql.and
import com.yusufteker.pulse.server.database.tables.BookmarkEntity
import com.yusufteker.pulse.server.database.tables.BookmarksTable
import com.yusufteker.pulse.server.database.tables.UserEntity
import java.time.Instant

fun Route.postRoutes() {
    authenticate("auth-jwt") {
        route("/posts") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = ((page - 1) * limit).toLong()

                val topicFilter = call.request.queryParameters["topic"]

                val posts = dbQuery {
                    val query = if (topicFilter != null) {
                        PostEntity.find { PostsTable.topic eq topicFilter }
                    } else {
                        PostEntity.all()
                    }

                    val entities = query
                        .orderBy(PostsTable.createdAt to SortOrder.DESC)
                        .limit(n = limit, offset = offset)
                        .toList()

                    entities.map { entity ->
                        val author = entity.author
                        val isBookmarkedByMe = if (userId != null) {
                            BookmarkEntity.find { 
                                (BookmarksTable.postId eq entity.id) and (BookmarksTable.userId eq userId) 
                            }.count() > 0
                        } else false

                        PostDto(
                            id = entity.id.value.toString(),
                            authorId = author.id.value.toString(),
                            authorName = author.name,
                            authorUsername = author.name.lowercase().replace(" ", "_"),
                            content = entity.content,
                            createdAt = entity.createdAt.toEpochMilli(),
                            likesCount = entity.likesCount,
                            commentsCount = entity.commentsCount,
                            isLikedByMe = false,
                            isBookmarkedByMe = isBookmarkedByMe,
                            topic = entity.topic
                        )
                    }
                }

                val hasMore = posts.size == limit
                
                call.respond(
                    HttpStatusCode.OK, 
                    FeedResponse(
                        posts = posts,
                        nextCursor = if (hasMore) (page + 1).toString() else null,
                        hasMore = hasMore
                    )
                )
            }

            post {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }

                val request = call.receiveNullable<com.yusufteker.pulse.shared.api.CreatePostRequest>()
                if (request == null || request.content.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Content cannot be empty")
                    return@post
                }

                dbQuery {
                    val user = com.yusufteker.pulse.server.database.tables.UserEntity.findById(userId)
                    if (user == null) {
                        return@dbQuery
                    }

                    PostEntity.new {
                        this.author = user
                        this.content = request.content
                        this.topic = request.topic ?: "GENERAL"
                        this.createdAt = java.time.Instant.now()
                        this.likesCount = 0
                        this.commentsCount = 0
                    }
                }

                call.respond(HttpStatusCode.Created, "Post created successfully")
            }

            post("/{postId}/bookmark") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val postIdStr = call.parameters["postId"]
                
                val postId = try {
                    java.util.UUID.fromString(postIdStr)
                } catch (e: Exception) {
                    null
                }

                if (userId == null || postId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@post
                }

                dbQuery {
                    val user = UserEntity.findById(userId)
                    val post = PostEntity.findById(postId)
                    
                    if (user == null || post == null) {
                        return@dbQuery
                    }

                    val existingBookmark = BookmarkEntity.find { 
                        (BookmarksTable.userId eq userId) and (BookmarksTable.postId eq postId) 
                    }.firstOrNull()

                    if (existingBookmark != null) {
                        existingBookmark.delete() // Unbookmark
                    } else {
                        BookmarkEntity.new {
                            this.user = user
                            this.post = post
                            this.createdAt = Instant.now()
                        }
                    }
                }

                call.respond(HttpStatusCode.OK)
            }
        }

        route("/bookmarks") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val posts = dbQuery {
                    val bookmarks = BookmarkEntity.find { BookmarksTable.userId eq userId }
                        .orderBy(BookmarksTable.createdAt to SortOrder.DESC)
                        .toList()

                    bookmarks.map { bookmark ->
                        val entity = bookmark.post
                        val author = entity.author
                        PostDto(
                            id = entity.id.value.toString(),
                            authorId = author.id.value.toString(),
                            authorName = author.name,
                            authorUsername = author.name.lowercase().replace(" ", "_"),
                            content = entity.content,
                            createdAt = entity.createdAt.toEpochMilli(),
                            likesCount = entity.likesCount,
                            commentsCount = entity.commentsCount,
                            isLikedByMe = false,
                            isBookmarkedByMe = true,
                            topic = entity.topic
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, FeedResponse(posts = posts, nextCursor = null, hasMore = false))
            }
        }
    }
}
