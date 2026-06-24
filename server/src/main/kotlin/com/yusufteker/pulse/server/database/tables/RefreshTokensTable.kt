package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Definition of the `refresh_tokens` table for Exposed ORM.
 */
object RefreshTokensTable : IntIdTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
}

/**
 * DAO representing a single row in the `refresh_tokens` table.
 */
class RefreshTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RefreshTokenEntity>(RefreshTokensTable)
    
    var user by UserEntity referencedOn RefreshTokensTable.userId
    var token by RefreshTokensTable.token
    var expiresAt by RefreshTokensTable.expiresAt
    var createdAt by RefreshTokensTable.createdAt
}
