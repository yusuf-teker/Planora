package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.*
import com.yusufteker.planora.server.service.CloudinaryService
import com.yusufteker.planora.shared.api.RoomMemberRole
import com.yusufteker.planora.shared.api.UserProfileResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.request.receiveMultipart
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.http.content.forEachPart
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.time.Instant

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        route("/users") {

            post("/profile-image") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }

                val multipartData = call.receiveMultipart()
                var imageBytes: ByteArray? = null

                multipartData.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        imageBytes = part.streamProvider().readBytes()
                    }
                    part.dispose()
                }

                if (imageBytes == null) {
                    call.respond(HttpStatusCode.BadRequest, "No image found")
                    return@post
                }

                var oldProfileUrl: String? = null
                dbQuery {
                    val user = UserEntity.findById(currentUserId)
                    oldProfileUrl = user?.profileImageUrl
                }

                val secureUrl = try {
                    com.yusufteker.planora.server.service.CloudinaryService.uploadProfileImage(imageBytes!!, currentUserId)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to upload image: ${e.message}")
                    return@post
                }

                if (!oldProfileUrl.isNullOrBlank()) {
                    CloudinaryService.deleteImageByUrl(oldProfileUrl!!)
                }

                dbQuery {
                    val user = UserEntity.findById(currentUserId)
                    user?.profileImageUrl = secureUrl
                }

                call.respond(HttpStatusCode.OK, mapOf("profileImageUrl" to secureUrl))
            }

            get("/search") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val query = call.request.queryParameters["q"]?.lowercase()?.trim() ?: ""

                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                if (query.isBlank()) {
                    call.respond(HttpStatusCode.OK, com.yusufteker.planora.shared.api.SearchUsersResponse(emptyList()))
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
                        val postsCount = com.yusufteker.planora.server.database.tables.PostEntity.find { com.yusufteker.planora.server.database.tables.PostsTable.authorId eq user.id.value }.count().toInt()
                        
                        val followRequest = com.yusufteker.planora.server.database.tables.FollowRequestEntity.find {
                            (com.yusufteker.planora.server.database.tables.FollowRequestsTable.requesterId eq currentUserId) and
                            (com.yusufteker.planora.server.database.tables.FollowRequestsTable.targetId eq user.id.value)
                        }.firstOrNull()
                        
                        val calendarAccess = CalendarAccessEntity.find {
                            (CalendarAccessTable.requesterId eq currentUserId) and
                            (CalendarAccessTable.granterId eq user.id.value)
                        }.firstOrNull()

                        UserProfileResponse(
                            id = user.id.value,
                            name = user.name,
                            email = user.email,
                            avatarId = user.avatarId,
                            followersCount = followersCount,
                            followingCount = followingCount,
                            postsCount = postsCount,
                            isFollowedByMe = followedUserIds.contains(user.id.value),
                            followRequestStatus = followRequest?.status,
                            calendarAccessStatus = calendarAccess?.status,
                            username = user.username,
                            profileImageUrl = user.profileImageUrl,
                            isPremium = user.isPremiumActive(),
                            premiumUntil = user.premiumUntil?.toString()
                        )
                    }
                    com.yusufteker.planora.shared.api.SearchUsersResponse(usersResponse)
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
                    val postsCount = com.yusufteker.planora.server.database.tables.PostEntity.find { com.yusufteker.planora.server.database.tables.PostsTable.authorId eq targetUserId }.count().toInt()
                    val isFollowedByMe = FollowerEntity.find { 
                        (FollowersTable.followerId eq currentUserId) and (FollowersTable.followedId eq targetUserId) 
                    }.count() > 0

                    val followRequest = com.yusufteker.planora.server.database.tables.FollowRequestEntity.find {
                        (com.yusufteker.planora.server.database.tables.FollowRequestsTable.requesterId eq currentUserId) and
                        (com.yusufteker.planora.server.database.tables.FollowRequestsTable.targetId eq targetUserId)
                    }.firstOrNull()
                    
                    val calendarAccess = CalendarAccessEntity.find {
                        (CalendarAccessTable.requesterId eq currentUserId) and
                        (CalendarAccessTable.granterId eq targetUserId)
                    }.firstOrNull()

                    UserProfileResponse(
                        id = user.id.value,
                        name = user.name,
                        email = user.email,
                        avatarId = user.avatarId,
                        followersCount = followersCount,
                        followingCount = followingCount,
                        postsCount = postsCount,
                        isFollowedByMe = isFollowedByMe,
                        followRequestStatus = followRequest?.status,
                        calendarAccessStatus = calendarAccess?.status,
                        username = user.username,
                        profileImageUrl = user.profileImageUrl,
                        isPremium = user.isPremiumActive(),
                        premiumUntil = user.premiumUntil?.toString()
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

                var isNewRequestSent = false
                var currentUsername = ""

                dbQuery {
                    val targetUser = UserEntity.findById(targetUserId)
                    val currentUser = UserEntity.findById(currentUserId)
                    
                    if (targetUser == null || currentUser == null) {
                        return@dbQuery
                    }

                    // Check if already following
                    val existingFollow = FollowerEntity.find { 
                        (FollowersTable.followerId eq currentUserId) and (FollowersTable.followedId eq targetUserId) 
                    }.firstOrNull()

                    if (existingFollow != null) {
                        existingFollow.delete() // Unfollow
                        
                        // İleride tekrar istek atabilmesi için varsa eski isteği de temizle
                        val existingRequest = com.yusufteker.planora.server.database.tables.FollowRequestEntity.find {
                            (com.yusufteker.planora.server.database.tables.FollowRequestsTable.requesterId eq currentUserId) and
                            (com.yusufteker.planora.server.database.tables.FollowRequestsTable.targetId eq targetUserId)
                        }.firstOrNull()
                        existingRequest?.delete()
                        
                        return@dbQuery
                    }

                    // Check if a request already exists (regardless of status, just in case old ACCEPTED ones exist)
                    val existingRequest = com.yusufteker.planora.server.database.tables.FollowRequestEntity.find {
                        (com.yusufteker.planora.server.database.tables.FollowRequestsTable.requesterId eq currentUserId) and
                        (com.yusufteker.planora.server.database.tables.FollowRequestsTable.targetId eq targetUserId)
                    }.firstOrNull()

                    if (existingRequest != null) {
                        existingRequest.delete() // Cancel request (or clear old stuck request)
                    } else {
                        com.yusufteker.planora.server.database.tables.FollowRequestEntity.new {
                            requester = currentUser
                            target = targetUser
                            status = "PENDING"
                            createdAt = Instant.now()
                        } // Send request
                        
                        isNewRequestSent = true
                        currentUsername = currentUser.username
                    }
                }

                if (isNewRequestSent) {
                    // Send push notification to target user
                    com.yusufteker.planora.server.service.FcmService.sendPushToUser(
                        userId = targetUserId,
                        title = "Yeni Takip İsteği",
                        body = "@$currentUsername seni takip etmek istiyor.",
                        data = mapOf("type" to "follow_request")
                    )
                }

                call.respond(HttpStatusCode.OK)
            }

            post("/{userId}/remove-follower") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val followerId = call.parameters["userId"]?.toIntOrNull()

                if (currentUserId == null || followerId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                    return@post
                }

                dbQuery {
                    val existingFollow = FollowerEntity.find { 
                        (FollowersTable.followerId eq followerId) and (FollowersTable.followedId eq currentUserId) 
                    }.firstOrNull()
                    existingFollow?.delete()
                }
                call.respond(HttpStatusCode.OK)
            }

            get("/followers") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                val followers = dbQuery {
                    FollowerEntity.find { FollowersTable.followedId eq currentUserId }
                        .map {
                            val user = it.follower
                            val followersCount = FollowerEntity.find { FollowersTable.followedId eq user.id.value }.count().toInt()
                            val followingCount = FollowerEntity.find { FollowersTable.followerId eq user.id.value }.count().toInt()
                            val postsCount = com.yusufteker.planora.server.database.tables.PostEntity.find { com.yusufteker.planora.server.database.tables.PostsTable.authorId eq user.id.value }.count().toInt()
                            val isFollowedByMe = FollowerEntity.find {
                                (FollowersTable.followerId eq currentUserId) and (FollowersTable.followedId eq user.id.value)
                            }.count() > 0
                            
                            val calendarAccess = CalendarAccessEntity.find {
                                (CalendarAccessTable.requesterId eq currentUserId) and
                                (CalendarAccessTable.granterId eq user.id.value)
                            }.firstOrNull()

                            UserProfileResponse(
                                id = user.id.value,
                                name = user.name,
                                username = user.username,
                                email = user.email,
                                followersCount = followersCount,
                                followingCount = followingCount,
                                isFollowedByMe = isFollowedByMe,
                                avatarId = user.avatarId,
                                postsCount = postsCount,
                                calendarAccessStatus = calendarAccess?.status,
                                profileImageUrl = user.profileImageUrl,
                                isPremium = user.isPremiumActive(),
                                premiumUntil = user.premiumUntil?.toString()
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, followers)
            }

            get("/follow-requests") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                val requests = dbQuery {
                    com.yusufteker.planora.server.database.tables.FollowRequestEntity.find {
                        (com.yusufteker.planora.server.database.tables.FollowRequestsTable.targetId eq currentUserId) and
                        (com.yusufteker.planora.server.database.tables.FollowRequestsTable.status eq "PENDING")
                    }.map {
                        val requester = it.requester
                        com.yusufteker.planora.shared.api.FollowRequestResponse(
                            id = it.id.value,
                            requesterId = requester.id.value,
                            requesterName = requester.name,
                            requesterUsername = requester.username,
                            requesterAvatarId = requester.avatarId,
                            status = it.status,
                            requesterProfileImageUrl = requester.profileImageUrl
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, requests)
            }

            post("/follow-requests/{requestId}/accept") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val requestId = call.parameters["requestId"]?.toIntOrNull()
                if (currentUserId == null || requestId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid Request ID")
                    return@post
                }

                var pushRequesterId: Int? = null
                var pushTargetUsername: String = ""

                dbQuery {
                    val request = com.yusufteker.planora.server.database.tables.FollowRequestEntity.findById(requestId)
                    if (request != null && request.target.id.value == currentUserId && request.status == "PENDING") {
                        FollowerEntity.new {
                            follower = request.requester
                            followed = request.target
                            createdAt = Instant.now()
                        }
                        pushRequesterId = request.requester.id.value
                        pushTargetUsername = request.target.username
                        request.delete() // Accept the request by creating follower and deleting request
                    }
                }

                if (pushRequesterId != null) {
                    com.yusufteker.planora.server.service.FcmService.sendPushToUser(
                        userId = pushRequesterId,
                        title = "Takip İsteği Kabul Edildi",
                        body = "@$pushTargetUsername takip isteğinizi kabul etti.",
                        data = mapOf("type" to "follow_request_accepted")
                    )
                }

                call.respond(HttpStatusCode.OK)
            }

            post("/follow-requests/{requestId}/reject") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val requestId = call.parameters["requestId"]?.toIntOrNull()
                if (currentUserId == null || requestId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid Request ID")
                    return@post
                }

                dbQuery {
                    val request = FollowRequestEntity.findById(requestId)
                    if (request != null && request.target.id.value == currentUserId) {
                        request.delete() // Just delete it if rejected
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
                            
                            val postsCount = com.yusufteker.planora.server.database.tables.PostEntity.find { com.yusufteker.planora.server.database.tables.PostsTable.authorId eq user.id.value }.count().toInt()
                            
                            val calendarAccess = CalendarAccessEntity.find {
                                (CalendarAccessTable.requesterId eq currentUserId) and
                                (CalendarAccessTable.granterId eq user.id.value)
                            }.firstOrNull()
                            
                            UserProfileResponse(
                                id = user.id.value,
                                name = user.name,
                                username = user.username,
                                email = user.email,
                                followersCount = followersCount,
                                followingCount = followingCount,
                                isFollowedByMe = true, // We are already querying followings
                                avatarId = user.avatarId,
                                postsCount = postsCount,
                                calendarAccessStatus = calendarAccess?.status,
                                profileImageUrl = user.profileImageUrl,
                                isPremium = user.isPremiumActive(),
                                premiumUntil = user.premiumUntil?.toString()
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, followingUsers)
            }

            // DELETE /users/me
            // Kullanıcı hesabını ve tüm ilişkili verilerini siler.
            // Odaların ve ortak görevlerin kuruculuğu devredilir.
            delete("/me") {
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal?.payload?.getClaim("userId")?.asInt()

                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@delete
                }

                dbQuery {
                    deleteUserAccountInternal(currentUserId)
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Account deleted successfully"))
            }
        }
    }
}

/**
 * Kullanıcının veritabanındaki tüm ilişkilerini temizleyen ve kuruculukları devreden dahili yardımcı fonksiyon.
 */
private fun deleteUserAccountInternal(targetUserId: Int) {
    val targetUserEntity = UserEntity.findById(targetUserId)

    // 1. Odalar & Üyelikler: Kurucusu olduğu odalarda devir yap
    val roomsCreatedByUser = PlanRoomEntity.find { PlanRoomsTable.creatorId eq targetUserId }
    for (room in roomsCreatedByUser) {
        val otherMemberRow = PlanRoomMembersTable
            .selectAll()
            .where { (PlanRoomMembersTable.roomId eq room.id.value) and (PlanRoomMembersTable.userId neq targetUserId) }
            .orderBy(PlanRoomMembersTable.joinedAt to org.jetbrains.exposed.sql.SortOrder.ASC)
            .firstOrNull()

        if (otherMemberRow != null) {
            val nextMemberId: Int = otherMemberRow[PlanRoomMembersTable.userId]
            val nextUser = UserEntity.findById(nextMemberId)
            if (nextUser != null) {
                room.creator = nextUser
            }
            PlanRoomMembersTable.update({ (PlanRoomMembersTable.roomId eq room.id.value) and (PlanRoomMembersTable.userId eq nextMemberId) }) {
                it[role] = RoomMemberRole.ADMIN
            }
        } else {
            TaskSharedRoomsTable.deleteWhere { roomId eq room.id.value }
            PlanRoomMembersTable.deleteWhere { roomId eq room.id.value }
            room.delete()
        }
    }
    PlanRoomMembersTable.deleteWhere { userId eq targetUserId }

    // 2. Görevler & Notlar: Kurucusu olduğu görevlerde devir/silme yap
    val tasksCreatedByUser = TaskEntity.find { TasksTable.creatorId eq targetUserId }
    for (task in tasksCreatedByUser) {
        val otherParticipantRow = TaskParticipantsTable
            .selectAll()
            .where { (TaskParticipantsTable.taskId eq task.id.value) and (TaskParticipantsTable.userId neq targetUserId) }
            .firstOrNull()

        if (otherParticipantRow != null) {
            val nextParticipantId: Int = otherParticipantRow[TaskParticipantsTable.userId]
            val nextUser = UserEntity.findById(nextParticipantId)
            if (nextUser != null) {
                task.creator = nextUser
            }
            TaskParticipantsTable.deleteWhere { (taskId eq task.id.value) and (userId eq targetUserId) }
        } else {
            TaskSharedRoomsTable.deleteWhere { taskId eq task.id.value }
            TaskParticipantsTable.deleteWhere { taskId eq task.id.value }
            task.delete()
        }
    }
    TaskParticipantsTable.deleteWhere { userId eq targetUserId }

    // 3. Postlar, Yorumlar, Favoriler
    val userPosts = PostEntity.find { PostsTable.authorId eq targetUserId }
    val userPostIds = userPosts.map { it.id }
    CommentsTable.deleteWhere { authorId eq targetUserId }
    if (userPostIds.isNotEmpty()) {
        CommentsTable.deleteWhere { postId inList userPostIds }
        BookmarksTable.deleteWhere { postId inList userPostIds }
    }
    BookmarksTable.deleteWhere { userId eq targetUserId }
    userPosts.forEach { it.delete() }

    // 4. Takipçiler ve İstekler
    FollowersTable.deleteWhere { (followerId eq targetUserId) or (followedId eq targetUserId) }
    FollowRequestsTable.deleteWhere { (requesterId eq targetUserId) or (targetId eq targetUserId) }

    // 5. Takvim Erişimi, Tokenlar ve Kullanıcı Kaydı
    CalendarAccessTable.deleteWhere { (requesterId eq targetUserId) or (granterId eq targetUserId) }
    FcmTokensTable.deleteWhere { userId eq targetUserId }
    RefreshTokensTable.deleteWhere { userId eq targetUserId }
    PasswordResetTokensTable.deleteWhere { userId eq targetUserId }
    targetUserEntity?.delete()
}


