package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.FollowerEntity
import com.yusufteker.pulse.server.database.tables.FollowersTable
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.server.database.tables.UsersTable
import com.yusufteker.pulse.shared.api.UserProfileResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.LikeEscapeOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.Expression
import java.time.Instant

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        route("/users") {

            get("/search") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val query = call.request.queryParameters["q"]?.lowercase()?.trim() ?: ""

                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                if (query.isBlank()) {
                    call.respond(HttpStatusCode.OK, com.yusufteker.pulse.shared.api.SearchUsersResponse(emptyList()))
                    return@get
                }

                val response = dbQuery {
                    // Bulduğumuz kullanıcıları listeleyeceğiz. Arama query'sini name veya username alanlarında arıyoruz.
                    // Kendi kendimizi sonuçlarda göstermemek için filtre ekliyoruz.
                    val searchResult = UserEntity.find {
                        (UsersTable.id neq currentUserId) and
                        ((UsersTable.name.lowerCase() like "%$query%") or (UsersTable.username.lowerCase() like "%$query%"))
                    }.limit(50).toList()

                    // Takip edilip edilmediklerini bulmak için
                    val followedUserIds = FollowerEntity.find { FollowersTable.followerId eq currentUserId }
                        .map { it.followed.id.value }
                        .toSet()

                    val usersResponse = searchResult.map { user ->
                        val followersCount = FollowerEntity.find { FollowersTable.followedId eq user.id.value }.count().toInt()
                        val followingCount = FollowerEntity.find { FollowersTable.followerId eq user.id.value }.count().toInt()
                        val postsCount = com.yusufteker.pulse.server.database.tables.PostEntity.find { com.yusufteker.pulse.server.database.tables.PostsTable.authorId eq user.id.value }.count().toInt()
                        
                        UserProfileResponse(
                            id = user.id.value,
                            name = user.name,
                            email = user.email,
                            avatarId = user.avatarId,
                            followersCount = followersCount,
                            followingCount = followingCount,
                            postsCount = postsCount,
                            isFollowedByMe = followedUserIds.contains(user.id.value),
                            username = user.username
                        )
                    }
                    com.yusufteker.pulse.shared.api.SearchUsersResponse(usersResponse)
                }

                call.respond(HttpStatusCode.OK, response)
            }

            // /users/{userId}/profile endpoint'i, kullanıcı profili bilgilerini döndürür.
            get("/{userId}/profile") {
                // Kullanıcı kimliğini JWT token'dan alıyoruz.
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()

                // URL parametresinden userId değerini alıyoruz. Eğer "me" ise, currentUserId kullanılır.
                val userIdParam = call.parameters["userId"]
                
                val targetUserId = if (userIdParam == "me") currentUserId else userIdParam?.toIntOrNull()

                if (currentUserId == null || targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                    return@get
                }

                val response = dbQuery {
                    val user = UserEntity.findById(targetUserId) ?: return@dbQuery null
                    
                    val followersCount = FollowerEntity.find { FollowersTable.followedId eq targetUserId }.count().toInt()
                    val followingCount = FollowerEntity.find { FollowersTable.followerId eq targetUserId }.count().toInt()
                    val postsCount = com.yusufteker.pulse.server.database.tables.PostEntity.find { com.yusufteker.pulse.server.database.tables.PostsTable.authorId eq targetUserId }.count().toInt()
                    val isFollowedByMe = FollowerEntity.find { 
                        (FollowersTable.followerId eq currentUserId) and (FollowersTable.followedId eq targetUserId) 
                    }.count() > 0

                    UserProfileResponse(
                        id = user.id.value,
                        name = user.name,
                        email = user.email,
                        avatarId = user.avatarId,
                        followersCount = followersCount,
                        followingCount = followingCount,
                        postsCount = postsCount,
                        isFollowedByMe = isFollowedByMe,
                        username = user.username
                    )
                }

                if (response != null) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }

            post("/{userId}/follow") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val targetUserId = call.parameters["userId"]?.toIntOrNull()

                if (currentUserId == null || targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                    return@post
                }
                
                if (currentUserId == targetUserId) {
                    call.respond(HttpStatusCode.BadRequest, "Cannot follow yourself")
                    return@post
                }

                dbQuery {
                    val targetUser = UserEntity.findById(targetUserId)
                    val currentUser = UserEntity.findById(currentUserId)
                    
                    if (targetUser == null || currentUser == null) {
                        return@dbQuery
                    }

                    val existingFollow = FollowerEntity.find { 
                        (FollowersTable.followerId eq currentUserId) and (FollowersTable.followedId eq targetUserId) 
                    }.firstOrNull()

                    if (existingFollow != null) {
                        existingFollow.delete() // Unfollow
                    } else {
                        FollowerEntity.new {
                            follower = currentUser
                            followed = targetUser
                            createdAt = Instant.now()
                        } // Follow
                    }
                }

                call.respond(HttpStatusCode.OK)
            }

            // GET /users/following
            // Takip ettiğin kişilerin listesini döndürür.
            get("/following") {
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal?.payload?.getClaim("userId")?.asInt()

                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                val followingUsers = dbQuery {
                    FollowerEntity.find { FollowersTable.followerId eq currentUserId }
                        .map {
                            val user = it.followed
                            val followersCount = FollowerEntity.find { FollowersTable.followedId eq user.id.value }.count().toInt()
                            val followingCount = FollowerEntity.find { FollowersTable.followerId eq user.id.value }.count().toInt()
                            
                            val postsCount = com.yusufteker.pulse.server.database.tables.PostEntity.find { com.yusufteker.pulse.server.database.tables.PostsTable.authorId eq user.id.value }.count().toInt()
                            
                            UserProfileResponse(
                                id = user.id.value,
                                name = user.name,
                                username = user.username,
                                email = user.email,
                                followersCount = followersCount,
                                followingCount = followingCount,
                                isFollowedByMe = true, // We are already querying followings
                                avatarId = user.avatarId,
                                postsCount = postsCount
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, followingUsers)
            }
        }
    }
}
