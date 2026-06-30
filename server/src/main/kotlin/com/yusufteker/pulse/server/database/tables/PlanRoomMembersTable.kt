package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.sql.Table
import com.yusufteker.pulse.shared.api.RoomMemberStatus
import com.yusufteker.pulse.shared.api.RoomMemberRole

/**
 * Plan Odasına üye olan kullanıcıların durumlarını ve yetkilerini tutan tablo.
 * Bir odada birden fazla kişi olabilir ve her kişinin durumu farklıdır (Davet beklemede, kabul etti vb.)
 */
object PlanRoomMembersTable : Table("plan_room_members") {
    // Hangi odaya ait olduğu bilgisi. 
    val roomId = varchar("room_id", 36).references(PlanRoomsTable.id)
    
    // Odaya dahil olan/davet edilen kullanıcının ID'si.
    val userId = integer("user_id").references(UsersTable.id)
    
    // Kullanıcının odadaki güncel durumu. (PENDING: Davet edildi, ACCEPTED: Katıldı, DECLINED: Reddetti)
    val status = enumerationByName("status", 50, RoomMemberStatus::class)
    
    // Kullanıcının odadaki rolü. (ADMIN: Odayı yönetebilir, kişi ekleyip çıkarabilir, MEMBER: Sadece planları görebilir/ekleyebilir)
    val role = enumerationByName("role", 50, RoomMemberRole::class)
    
    // Kullanıcının daveti kabul edip odaya katıldığı zaman. (Eğer hala davet durumundaysa null olabilir)
    val joinedAt = long("joined_at").nullable()

    // Bir odada bir kullanıcı yalnızca bir kez bulunabilir, bu yüzden iki alanın birleşimi (Composite Key) Primary Key olur.
    override val primaryKey = PrimaryKey(roomId, userId)
}
