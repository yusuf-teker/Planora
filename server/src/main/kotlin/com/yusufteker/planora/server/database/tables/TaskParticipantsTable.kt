package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Görevlere ve etkinliklere katılan kullanıcıların RSVP durumlarını tutar.
 */
object TaskParticipantsTable : Table("task_participants") {
    val taskId = varchar("task_id", 100)
    val userId = integer("user_id").references(UsersTable.id)
    val status = varchar("status", 50) // PENDING, ACCEPTED, DECLINED
    val reminders = text("reminders").nullable()

    override val primaryKey = PrimaryKey(taskId, userId)
}
