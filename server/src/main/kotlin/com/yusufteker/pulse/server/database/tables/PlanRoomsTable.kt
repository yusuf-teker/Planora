package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column

/**
 * Plan Odası (Ortak Takvim) verilerini tutan tablo.
 * Kullanıcıların ortak planlarını yönettikleri alanın ana kayıt noktasıdır.
 */
object PlanRoomsTable : IdTable<String>("plan_rooms") {
    // Odanın benzersiz kimliği (UUID). İstemcide (local db) ve sunucuda veriyi eşleştirmek için kullanılır.
    override val id: Column<EntityID<String>> = varchar("id", 100).entityId()
    
    // Odanın görünen adı (Örn: "Haftasonu Tatili", "Yusuf & Dilber Planları")
    val name = varchar("name", 255)
    
    // Odayı ilk oluşturan kullanıcının ID'si. Bu kişi otomatik olarak odanın ADMIN'i olur.
    val creatorId = reference("creator_id", UsersTable)
    
    // Odanın oluşturulma tarihi (Unix timestamp formatında). Sıralama ve geçmiş verileri çekerken kullanılır.
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

class PlanRoomEntity(id: org.jetbrains.exposed.dao.id.EntityID<String>) : org.jetbrains.exposed.dao.Entity<String>(id) {
    companion object : org.jetbrains.exposed.dao.EntityClass<String, PlanRoomEntity>(PlanRoomsTable)

    var name by PlanRoomsTable.name
    var creator by UserEntity referencedOn PlanRoomsTable.creatorId
    var createdAt by PlanRoomsTable.createdAt
}
