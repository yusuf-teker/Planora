package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Definition of the `email_verification_tokens` table for Exposed ORM.
 *
 * Stores temporary 6-digit OTP codes for new user registration email verification.
 */
object EmailVerificationTokensTable : IntIdTable("email_verification_tokens") {
    val email = varchar("email", 255).index()
    val token = varchar("token", 6)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
}

/**
 * DAO representing a single row in the `email_verification_tokens` table.
 */
class EmailVerificationTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EmailVerificationTokenEntity>(EmailVerificationTokensTable)

    var email by EmailVerificationTokensTable.email
    var token by EmailVerificationTokensTable.token
    var expiresAt by EmailVerificationTokensTable.expiresAt
    var createdAt by EmailVerificationTokensTable.createdAt
}
