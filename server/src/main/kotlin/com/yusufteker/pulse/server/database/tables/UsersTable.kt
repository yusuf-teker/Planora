package com.yusufteker.pulse.server.database.tables

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
}
