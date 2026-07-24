package com.yusufteker.planora.core.database

fun PlanoraDatabaseQueries.clearAll() {
    transaction {
        clearAllPosts()
        clearAllRemoteKeys()
        clearAllPendingPosts()
        deleteAllPlanRooms()
        deleteAllPlanRoomMembers()
        deleteAllTasks()
        deleteAllTaskSharedRooms()
        deleteAllTaskExceptions()
        deleteAllCalendarAccess()
    }
}
