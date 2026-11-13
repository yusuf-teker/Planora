package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Exposed table mapping for user subscription and receipt records (`subscriptions`).
 */
object SubscriptionsTable : IntIdTable("subscriptions") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val appId = varchar("app_id", 50).default("ecosystem")
    val planId = varchar("plan_id", 100)
    val provider = varchar("provider", 50)
    val purchaseToken = text("purchase_token").nullable()
    val transactionId = varchar("transaction_id", 255).nullable()
    val status = varchar("status", 30).default("ACTIVE")
    val autoRenew = bool("auto_renew").default(true)
    val startedAt = timestamp("started_at").clientDefault { Instant.now() }
    val expiresAt = timestamp("expires_at").nullable()
    val rawPayload = text("raw_payload").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

class SubscriptionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SubscriptionEntity>(SubscriptionsTable)

    var user by UserEntity referencedOn SubscriptionsTable.userId
    var appId by SubscriptionsTable.appId
    var planId by SubscriptionsTable.planId
    var provider by SubscriptionsTable.provider
    var purchaseToken by SubscriptionsTable.purchaseToken
    var transactionId by SubscriptionsTable.transactionId
    var status by SubscriptionsTable.status
    var autoRenew by SubscriptionsTable.autoRenew
    var startedAt by SubscriptionsTable.startedAt
    var expiresAt by SubscriptionsTable.expiresAt
    var rawPayload by SubscriptionsTable.rawPayload
    var createdAt by SubscriptionsTable.createdAt
    var updatedAt by SubscriptionsTable.updatedAt
}

