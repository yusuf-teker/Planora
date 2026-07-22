package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Definition of the `password_reset_tokens` table for Exposed ORM.
 *
 * Stores temporary 6-digit OTP codes for password reset authentication.
 */
object PasswordResetTokensTable : IntIdTable("password_reset_tokens") {
    val userId = reference("user_id", UsersTable)
    val token = varchar("token", 6)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
}

/**
 * DAO representing a single row in the `password_reset_tokens` table.
 */
class PasswordResetTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PasswordResetTokenEntity>(PasswordResetTokensTable)

    var user by UserEntity referencedOn PasswordResetTokensTable.userId
    var token by PasswordResetTokensTable.token
    var expiresAt by PasswordResetTokensTable.expiresAt
    var createdAt by PasswordResetTokensTable.createdAt
}
