package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.PlanRoomEntity
import com.yusufteker.pulse.server.database.tables.PlanRoomMembersTable
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.shared.api.CreatePlanRoomRequest
import com.yusufteker.pulse.shared.api.InviteUserRequest
import com.yusufteker.pulse.shared.api.PlanRoomDto
import com.yusufteker.pulse.shared.api.PlanRoomMemberDto
import com.yusufteker.pulse.shared.api.RespondToInviteRequest
import com.yusufteker.pulse.shared.api.RoomMemberRole
import com.yusufteker.pulse.shared.api.RoomMemberStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.UUID

fun Route.planRoomRoutes() {
    authenticate("auth-jwt") {
        route("/rooms") {

            // 1. Kullanıcının dahil olduğu odaları listeleme
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val rooms = dbQuery {

                    // =========================================================================
                    // ADIM 1: KULLANICININ İÇİNDE OLDUĞU VE DAVETİNİ KABUL ETTİĞİ ODALARI BUL
                    // =========================================================================
                    // PlanRoomMembersTable, hangi kullanıcının hangi odada olduğunu tutan bir tablodur.
                    val acceptedRoomIds = PlanRoomMembersTable.selectAll().where {

                        // İstek atan kullanıcının ID'sini arıyoruz (Kullanıcı bu odada var mı?)
                        (PlanRoomMembersTable.userId eq userId) and

                                // Ve bu odadaki durumu 'ACCEPTED' (Kabul Edilmiş) mi?
                                // (Çünkü kullanıcı daveti reddetmiş veya bekletiyor olabilir, sadece aktif katıldığı odaları istiyoruz)
                                (PlanRoomMembersTable.status eq RoomMemberStatus.ACCEPTED)

                    }.map { it[PlanRoomMembersTable.roomId] } // Bulunan sonuçlardan sadece "Oda Numaralarını (ID)" alıp bir liste yapıyoruz.

                    // =========================================================================
                    // ADIM 2: EĞER HİÇ ODA YOKSA İŞLEMİ BİTİR
                    // =========================================================================
                    // Eğer kullanıcının kabul ettiği hiçbir oda yoksa, veritabanını daha fazla yormaya gerek yok.
                    // Hemen boş bir liste döndürüp işlemi bitiriyoruz.
                    if (acceptedRoomIds.isEmpty()) {
                        return@dbQuery emptyList<PlanRoomDto>()
                    }

                    // =========================================================================
                    // ADIM 3: BULUNAN ODALARDAKİ "TÜM ÜYELERİ" TEK SEFERDE ÇEK
                    // =========================================================================
                    // Elimizde kullanıcının dahil olduğu odaların ID'leri var (Örn: Oda 1, Oda 3).
                    // Şimdi bu odalarda bulunan HERKESİ (sadece bizi değil, odadaki diğer insanları da) tek bir sorguyla çekiyoruz.
                    // Bunu yapmamızın sebebi: Her oda için ayrı ayrı sorgu atmak veritabanını çok yorar (Buna N+1 problemi denir).
                    val allMembers = PlanRoomMembersTable.selectAll().where { // Tüm üyelerin durumlarını çekiyoruz
                        PlanRoomMembersTable.roomId inList acceptedRoomIds // Sadece kullanıcının dahil olduğu odalara ait üyeleri çekiyoruz
                    }.toList()

                    // =========================================================================
                    // ADIM 4: ODALARI VE ÜYELERİNİ BİRLEŞTİRİP SONUÇ LİSTESİ OLUŞTUR
                    // =========================================================================
                    // Elimizdeki her bir oda ID'si için sırayla şu işlemleri yapıyoruz:
                    acceptedRoomIds.mapNotNull { roomId ->

                        // 4.1: Odanın temel bilgilerini (adı, ne zaman kurulduğu vs.) veritabanından çekiyoruz.
                        // Eğer oda silinmişse veya bulunamazsa 'null' dönerek bu odayı atlıyoruz (return@mapNotNull null).
                        val roomEntity = PlanRoomEntity.findById(roomId) ?: return@mapNotNull null

                        // 4.2: Adım 3'te çektiğimiz torba halindeki "tüm üyeler" listesinden,
                        // sadece "şu an işlem yaptığımız odaya (roomId) ait olanları" filtreleyip ayıklıyoruz.
                        // Ayıkladığımız verileri (satırları), mobil uygulamanın anlayabileceği veri paketine (PlanRoomMemberDto) çeviriyoruz.
                        val roomMembers = allMembers.filter { it[PlanRoomMembersTable.roomId] == roomId }.map { memberRow ->
                            PlanRoomMemberDto(
                                roomId = memberRow[PlanRoomMembersTable.roomId],
                                userId = memberRow[PlanRoomMembersTable.userId],
                                status = memberRow[PlanRoomMembersTable.status],
                                role = memberRow[PlanRoomMembersTable.role],
                                joinedAt = memberRow[PlanRoomMembersTable.joinedAt]
                            )
                        }

                        // 4.3: Odanın temel bilgileri (roomEntity) ile o odanın ayıklanmış üyelerini (roomMembers)
                        // tek bir ana pakette (PlanRoomDto) birleştiriyoruz. Frontend'e gidecek olan asıl liste bu paketlerden oluşuyor.
                        PlanRoomDto(
                            id = roomId,
                            name = roomEntity.name,
                            creatorId = roomEntity.creator.id.value,
                            createdAt = roomEntity.createdAt,
                            members = roomMembers
                        )
                    }
                }


                call.respond(HttpStatusCode.OK, rooms)
            }

            // 2. Oda oluşturma
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }

                val request = call.receiveNullable<CreatePlanRoomRequest>()
                if (request == null || request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Room name cannot be empty")
                    return@post
                }

                val roomId = UUID.randomUUID().toString()

                val newRoom = dbQuery {
                    val user = UserEntity.findById(userId) ?: return@dbQuery null

                    // Create the PlanRoom
                    val room = PlanRoomEntity.new(roomId) {
                        this.name = request.name
                        this.creator = user
                        this.createdAt = Instant.now().toEpochMilli()
                    }
                    
                    // Flush it so it's inserted into the DB before we insert related records in PlanRoomMembersTable
                    room.flush()

                    // Add the creator as an ACCEPTED ADMIN member
                    PlanRoomMembersTable.insert {
                        it[PlanRoomMembersTable.roomId] = roomId
                        it[PlanRoomMembersTable.userId] = userId
                        it[status] = RoomMemberStatus.ACCEPTED
                        it[role] = RoomMemberRole.ADMIN
                        it[joinedAt] = Instant.now().toEpochMilli()
                    }

                    PlanRoomDto(
                        id = room.id.value,
                        name = room.name,
                        creatorId = user.id.value,
                        createdAt = room.createdAt,
                        members = listOf(
                            PlanRoomMemberDto(
                                roomId = roomId,
                                userId = userId,
                                status = RoomMemberStatus.ACCEPTED,
                                role = RoomMemberRole.ADMIN,
                                joinedAt = Instant.now().toEpochMilli()
                            )
                        )
                    )
                }

                if (newRoom != null) {
                    call.respond(HttpStatusCode.Created, newRoom)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create room")
                }
            }

            // 2. Odaya birini davet etme
            post("/{roomId}/invite") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val roomId = call.parameters["roomId"]
                
                if (userId == null || roomId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@post
                }

                val request = call.receiveNullable<InviteUserRequest>()
                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, "User ID to invite is missing")
                    return@post
                }

                val success = dbQuery {
                    val room = PlanRoomEntity.findById(roomId) ?: return@dbQuery false
                    val targetUser = UserEntity.findById(request.userId) ?: return@dbQuery false

                    // Check if current user is an admin of the room
                    val isAdmin = PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.roomId eq roomId) and 
                        (PlanRoomMembersTable.userId eq userId) and 
                        (PlanRoomMembersTable.role eq RoomMemberRole.ADMIN)
                    }.count() > 0

                    if (!isAdmin) return@dbQuery false

                    // Check if target user is already in the room (pending, accepted, etc.)
                    val alreadyMember = PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.roomId eq roomId) and 
                        (PlanRoomMembersTable.userId eq targetUser.id.value)
                    }.count() > 0

                    if (alreadyMember) return@dbQuery false

                    // Insert as PENDING
                    PlanRoomMembersTable.insert {
                        it[PlanRoomMembersTable.roomId] = roomId
                        it[PlanRoomMembersTable.userId] = targetUser.id.value
                        it[status] = RoomMemberStatus.PENDING
                        it[role] = RoomMemberRole.MEMBER
                        it[joinedAt] = null
                    }
                    true
                }

                if (success) {
                    call.respond(HttpStatusCode.OK, "User invited successfully")
                } else {
                    call.respond(HttpStatusCode.Forbidden, "Cannot invite user. Either not admin or user already invited.")
                }
            }

            // 3. Kullanıcının bekleyen davetlerini listeleme
            get("/invitations") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val invitations = dbQuery {
                    PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.userId eq userId) and 
                        (PlanRoomMembersTable.status eq RoomMemberStatus.PENDING)
                    }.map { row ->
                        val roomId = row[PlanRoomMembersTable.roomId]
                        val roomEntity = PlanRoomEntity.findById(roomId)
                        
                        PlanRoomDto(
                            id = roomId,
                            name = roomEntity?.name ?: "Unknown Room",
                            creatorId = roomEntity?.creator?.id?.value ?: 0,
                            createdAt = roomEntity?.createdAt ?: 0L,
                            members = PlanRoomMembersTable.selectAll().where {
                                PlanRoomMembersTable.roomId eq roomId
                            }.map { memberRow ->
                                PlanRoomMemberDto(
                                    roomId = memberRow[PlanRoomMembersTable.roomId],
                                    userId = memberRow[PlanRoomMembersTable.userId],
                                    status = memberRow[PlanRoomMembersTable.status],
                                    role = memberRow[PlanRoomMembersTable.role],
                                    joinedAt = memberRow[PlanRoomMembersTable.joinedAt]
                                )
                            }
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, invitations)
            }

            // 4. Daveti kabul etme veya reddetme
            post("/{roomId}/invitations/respond") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val roomId = call.parameters["roomId"]
                
                if (userId == null || roomId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@post
                }

                val request = call.receiveNullable<RespondToInviteRequest>()
                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, "Accept/Decline status missing")
                    return@post
                }

                val success = dbQuery {
                    val pendingInvite = PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.roomId eq roomId) and 
                        (PlanRoomMembersTable.userId eq userId) and 
                        (PlanRoomMembersTable.status eq RoomMemberStatus.PENDING)
                    }.count() > 0

                    if (!pendingInvite) return@dbQuery false

                    PlanRoomMembersTable.update({
                        (PlanRoomMembersTable.roomId eq roomId) and (PlanRoomMembersTable.userId eq userId)
                    }) {
                        it[status] = if (request.accept) RoomMemberStatus.ACCEPTED else RoomMemberStatus.DECLINED
                        if (request.accept) {
                            it[joinedAt] = Instant.now().toEpochMilli()
                        }
                    }
                    true
                }

                if (success) {
                    call.respond(HttpStatusCode.OK, "Invitation responded successfully")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Invitation not found or already responded")
                }
            }

        }
    }
}
