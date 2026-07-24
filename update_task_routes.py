import re

with open("server/src/main/kotlin/com/yusufteker/planora/server/routes/TaskRoutes.kt", "r") as f:
    content = f.read()

# 1. Update POST route for participants and push notification
post_participants_old = """                    // Insert participants
                    request.participants.keys.forEach { pId ->
                        TaskParticipantsTable.insert {
                            it[taskId] = newTaskId
                            it[TaskParticipantsTable.userId] = pId
                            it[status] = "PENDING"
                        }
                    }"""

post_participants_new = """                    // Insert participants
                    request.participants.keys.forEach { pId ->
                        TaskParticipantsTable.insert {
                            it[taskId] = newTaskId
                            it[TaskParticipantsTable.userId] = pId
                            it[status] = "PENDING"
                            if (pId == userId) {
                                it[reminders] = if (request.reminders.isNotEmpty()) kotlinx.serialization.json.Json.encodeToString(request.reminders) else null
                            }
                        }
                    }"""
content = content.replace(post_participants_old, post_participants_new)

post_sync_old = """                    // Trigger FCM sync for room members if shared
                    if (request.visibility == TaskVisibility.ROOM_SHARED && request.sharedRoomIds.isNotEmpty()) {
                        request.sharedRoomIds.forEach { roomId ->
                            routeScope.launch {
                                com.yusufteker.planora.server.service.FcmService.sendSyncTriggerToRoomMembers(roomId, excludeUserId = userId)
                            }
                        }
                    }"""

post_sync_new = """                    // Trigger FCM sync for room members if shared
                    if (request.visibility == TaskVisibility.ROOM_SHARED && request.sharedRoomIds.isNotEmpty()) {
                        request.sharedRoomIds.forEach { roomId ->
                            routeScope.launch {
                                com.yusufteker.planora.server.service.FcmService.sendSyncTriggerToRoomMembers(roomId, excludeUserId = userId)
                            }
                        }
                    }
                    
                    // Send push notifications to newly added participants
                    val toAdd = request.participants.keys.filter { it != userId }
                    if (toAdd.isNotEmpty()) {
                        val creatorName = org.jetbrains.exposed.sql.transactions.transaction {
                            com.yusufteker.planora.server.database.tables.UsersTable.selectAll().where { com.yusufteker.planora.server.database.tables.UsersTable.id eq userId }.firstOrNull()?.get(com.yusufteker.planora.server.database.tables.UsersTable.name) ?: "Birisi"
                        }
                        toAdd.forEach { addedUserId ->
                            routeScope.launch {
                                com.yusufteker.planora.server.service.FcmService.sendPushToUser(
                                    userId = addedUserId,
                                    title = "Yeni Görev",
                                    body = "$creatorName seni '${request.title}' planına ekledi."
                                )
                            }
                        }
                    }"""
content = content.replace(post_sync_old, post_sync_new)

# 2. Update PUT route
put_participants_old = """                            // Update participants
                            TaskParticipantsTable.deleteWhere { TaskParticipantsTable.taskId eq taskId }
                            request.participants.keys.forEach { pId ->
                                TaskParticipantsTable.insert {
                                    it[TaskParticipantsTable.taskId] = taskId
                                    it[TaskParticipantsTable.userId] = pId
                                    it[status] = "PENDING"
                                }
                            }"""

put_participants_new = """                            // Update participants safely
                            val existingParticipantIds = TaskParticipantsTable.selectAll()
                                .where { TaskParticipantsTable.taskId eq taskId }
                                .map { it[TaskParticipantsTable.userId] }
                            
                            val requestedParticipantIds = request.participants.keys
                            val toRemove = existingParticipantIds - requestedParticipantIds
                            val toAdd = requestedParticipantIds - existingParticipantIds

                            if (toRemove.isNotEmpty()) {
                                TaskParticipantsTable.deleteWhere { 
                                    (TaskParticipantsTable.taskId eq taskId) and (TaskParticipantsTable.userId inList toRemove) 
                                }
                            }

                            toAdd.forEach { pId ->
                                TaskParticipantsTable.insert {
                                    it[TaskParticipantsTable.taskId] = taskId
                                    it[TaskParticipantsTable.userId] = pId
                                    it[TaskParticipantsTable.status] = "PENDING"
                                }
                            }

                            // Update reminders for the current user if they are a participant
                            if (requestedParticipantIds.contains(userId)) {
                                TaskParticipantsTable.update({ (TaskParticipantsTable.taskId eq taskId) and (TaskParticipantsTable.userId eq userId) }) {
                                    it[TaskParticipantsTable.reminders] = if (request.reminders.isNotEmpty()) kotlinx.serialization.json.Json.encodeToString(request.reminders) else null
                                }
                            }
                            
                            // Send push notifications to newly added users
                            if (toAdd.isNotEmpty()) {
                                val creatorName = UsersTable.selectAll().where { UsersTable.id eq userId }.firstOrNull()?.get(UsersTable.name) ?: "Birisi"
                                toAdd.forEach { addedUserId ->
                                    if (addedUserId != userId) {
                                        routeScope.launch {
                                            com.yusufteker.planora.server.service.FcmService.sendPushToUser(
                                                userId = addedUserId,
                                                title = "Yeni Görev",
                                                body = "$creatorName seni '${request.title}' planına ekledi."
                                            )
                                        }
                                    }
                                }
                            }"""
content = content.replace(put_participants_old, put_participants_new)

with open("server/src/main/kotlin/com/yusufteker/planora/server/routes/TaskRoutes.kt", "w") as f:
    f.write(content)
