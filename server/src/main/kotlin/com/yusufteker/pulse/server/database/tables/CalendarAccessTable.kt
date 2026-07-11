package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object CalendarAccessTable : IntIdTable("calendar_access") {
    val requesterId = reference("requester_id", UsersTable)
    val granterId = reference("granter_id", UsersTable)
    val status = varchar("status", 20).default("PENDING") // PENDING, GRANTED
    val color = varchar("color", 20).nullable()
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(requesterId, granterId)
    }
}

class CalendarAccessEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CalendarAccessEntity>(CalendarAccessTable)

    var requester by UserEntity referencedOn CalendarAccessTable.requesterId
    var granter by UserEntity referencedOn CalendarAccessTable.granterId
    var status by CalendarAccessTable.status
    var color by CalendarAccessTable.color
    var createdAt by CalendarAccessTable.createdAt
}
