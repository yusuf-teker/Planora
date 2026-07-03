package com.yusufteker.pulse.core.database

fun PulsyDatabaseQueries.clearAll() {
    transaction {
        clearAllPosts()
        clearAllRemoteKeys()
        clearAllPendingPosts()
        deleteAllPlanRooms()
        deleteAllPlanRoomMembers()
        deleteAllTasks()
        deleteAllTaskSharedRooms()
        deleteAllTaskExceptions()
    }
}
