package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Plan Odası (Ortak Takvim) verilerini tutan tablo.
 * Kullanıcıların ortak planlarını yönettikleri alanın ana kayıt noktasıdır.
 */
object PlanRoomsTable : Table("plan_rooms") {
    // Odanın benzersiz kimliği (UUID). İstemcide (local db) ve sunucuda veriyi eşleştirmek için kullanılır.
    val id = varchar("id", 36)
    
    // Odanın görünen adı (Örn: "Haftasonu Tatili", "Yusuf & Dilber Planları")
    val name = varchar("name", 255)
    
    // Odayı ilk oluşturan kullanıcının ID'si. Bu kişi otomatik olarak odanın ADMIN'i olur.
    val creatorId = integer("creator_id").references(UsersTable.id)
    
    // Odanın oluşturulma tarihi (Unix timestamp formatında). Sıralama ve geçmiş verileri çekerken kullanılır.
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
