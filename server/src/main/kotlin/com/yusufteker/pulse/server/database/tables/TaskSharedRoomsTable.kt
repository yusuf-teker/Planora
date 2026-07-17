package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Hangi görevin, hangi odalarda paylaşıldığını (görünür olduğunu) tutan köprü (many-to-many) tablosu.
 * Eğer bir task'in görünürlüğü (visibility) ROOM_SHARED ise, hangi odalara atandığı bu tablodan bulunur.
 */
object TaskSharedRoomsTable : Table("task_shared_rooms") {
    // Paylaşılan görevin ID'si
    val taskId = varchar("task_id", 100).references(TasksTable.id)
    
    // Görevin görünür olduğu (paylaşıldığı) odanın ID'si
    val roomId = varchar("room_id", 100).references(PlanRoomsTable.id)

    // Bir görev aynı odada yalnızca bir kez paylaşılabilir (Composite Key)
    override val primaryKey = PrimaryKey(taskId, roomId)
}
