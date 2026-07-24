import re

with open("server/src/main/kotlin/com/yusufteker/planora/server/routes/TaskRoutes.kt", "r") as f:
    content = f.read()

# Replace TaskDto mapping in GET
get_mapping_old = """                            reminders = entity.reminders?.let { try { Json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),"""

get_mapping_new = """                            reminders = TaskParticipantsTable.selectAll()
                                .where { (TaskParticipantsTable.taskId eq entity.id.value) and (TaskParticipantsTable.userId eq userId) }
                                .firstOrNull()?.get(TaskParticipantsTable.reminders)?.let { 
                                    try { kotlinx.serialization.json.Json.decodeFromString<List<Int>>(it) } catch(e: Exception) { emptyList() } 
                                } ?: entity.reminders?.let { try { kotlinx.serialization.json.Json.decodeFromString<List<Int>>(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),"""

content = content.replace(get_mapping_old, get_mapping_new)

with open("server/src/main/kotlin/com/yusufteker/planora/server/routes/TaskRoutes.kt", "w") as f:
    f.write(content)
