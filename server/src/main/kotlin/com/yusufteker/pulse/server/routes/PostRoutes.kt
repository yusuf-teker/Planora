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

fun Route.postRoutes() {
    authenticate("auth-jwt") {
        route("/posts") {
            get {
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
        }
    }
}
