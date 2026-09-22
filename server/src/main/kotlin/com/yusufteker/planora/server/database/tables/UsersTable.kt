package com.yusufteker.planora.server.database.tables

import java.time.Instant
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Definition of the `users` table for Exposed ORM.
 */
object UsersTable : IntIdTable("users") {
    val name = varchar("name", 255)
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    val avatarId = varchar("avatar_id", 255).default("avatar_1")
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
    val isPremium = bool("is_premium").default(false)
    val premiumUntil = timestamp("premium_until").nullable()
}

/**
 * DAO (Data Access Object) representing a single row in the `users` table.
 */
class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(UsersTable)
    
    var name by UsersTable.name
    var username by UsersTable.username
    var email by UsersTable.email
    var passwordHash by UsersTable.passwordHash
    var createdAt by UsersTable.createdAt
    var avatarId by UsersTable.avatarId
    var profileImageUrl by UsersTable.profileImageUrl
    var isPremium by UsersTable.isPremium
    var premiumUntil by UsersTable.premiumUntil
}

/**
 * Checks whether the user's Premium status is active.
 * If a finite duration was assigned and expired, it resets the fields and returns false.
 */
fun UserEntity.isPremiumActive(): Boolean {
    if (!this.isPremium) return false
    val until = this.premiumUntil
    val now = Instant.now()
    return if (until != null && until.isBefore(now)) {
        this.isPremium = false
        this.premiumUntil = null
        false
    } else {
        true
    }
}

