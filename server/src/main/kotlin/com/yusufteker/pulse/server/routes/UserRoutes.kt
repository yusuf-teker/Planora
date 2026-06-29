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
import java.time.Instant

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        route("/users") {

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
                        isFollowedByMe = isFollowedByMe
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

            // endpoints for GET followers/following could be added here later if needed
        }
    }
}
