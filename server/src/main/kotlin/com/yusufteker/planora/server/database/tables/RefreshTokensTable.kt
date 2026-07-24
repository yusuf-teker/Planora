package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Definition of the `refresh_tokens` table for Exposed ORM.
 */

object RefreshTokensTable : IntIdTable("refresh_tokens") {
    // IntId sayesinde integer("id").autoIncrement().primaryKey() // otomatik id oluşturuyor yazmaya gerek yok
    val userId = reference("user_id", UsersTable) // refresh_tokens.user_id -> users.id foreign key
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
}

/**
 * DAO representing a single row in the `refresh_tokens` table.
 */
class RefreshTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    // Exposed içindeki IntEntityClass (içinde new ve find gibi fonksiyonlar var) ile RefreshTokensTable tablosuna bağlanıyoruz.
    companion object : IntEntityClass<RefreshTokenEntity>(RefreshTokensTable)
    
    var user by UserEntity referencedOn RefreshTokensTable.userId
    var token by RefreshTokensTable.token
    var expiresAt by RefreshTokensTable.expiresAt
    var createdAt by RefreshTokensTable.createdAt
}
