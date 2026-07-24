package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object FcmTokensTable : IntIdTable("fcm_tokens") {
    val userId = reference("user_id", UsersTable)
    val token = varchar("token", 512).uniqueIndex()
    val platform = varchar("platform", 50) // e.g., "android", "ios"
    val updatedAt = timestamp("updated_at")
}

class FcmTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FcmTokenEntity>(FcmTokensTable)

    var user by UserEntity referencedOn FcmTokensTable.userId
    var token by FcmTokensTable.token
    var platform by FcmTokensTable.platform
    var updatedAt by FcmTokensTable.updatedAt
}
