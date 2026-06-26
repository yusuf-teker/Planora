package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.PostEntity
import com.yusufteker.pulse.server.database.tables.PostsTable
import com.yusufteker.pulse.shared.api.FeedResponse
import com.yusufteker.pulse.shared.api.PostDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.SortOrder

fun Route.postRoutes() {
    authenticate("auth-jwt") {
        route("/posts") {
            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = ((page - 1) * limit).toLong()

                val posts = dbQuery {
                    val entities = PostEntity.all()
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
                            isLikedByMe = false
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
        }
    }
}
