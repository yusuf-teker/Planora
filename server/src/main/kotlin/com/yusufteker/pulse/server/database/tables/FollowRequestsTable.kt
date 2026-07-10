package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FollowRequestsTable : IntIdTable("follow_requests") {
    val requesterId = reference("requester_id", UsersTable)
    val targetId = reference("target_id", UsersTable)
    val status = varchar("status", 20).default("PENDING")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(requesterId, targetId)
    }
}

class FollowRequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FollowRequestEntity>(FollowRequestsTable)

    var requester by UserEntity referencedOn FollowRequestsTable.requesterId
    var target by UserEntity referencedOn FollowRequestsTable.targetId
    var status by FollowRequestsTable.status
    var createdAt by FollowRequestsTable.createdAt
}
