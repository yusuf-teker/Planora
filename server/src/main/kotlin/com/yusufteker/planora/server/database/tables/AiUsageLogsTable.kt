package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Definition of the `ai_usage_logs` table for Exposed ORM.
 * Used to track AI request quotas (daily and weekly) for users.
 */
object AiUsageLogsTable : IntIdTable("ai_usage_logs") {
    val userId = integer("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val actionType = varchar("action_type", 50).default("AI_CHAT")
    val createdAt = timestamp("created_at")
}

/**
 * DAO entity representing a single AI request log.
 */
class AiUsageLogEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AiUsageLogEntity>(AiUsageLogsTable)

    var userId by AiUsageLogsTable.userId
    var actionType by AiUsageLogsTable.actionType
    var createdAt by AiUsageLogsTable.createdAt
}
