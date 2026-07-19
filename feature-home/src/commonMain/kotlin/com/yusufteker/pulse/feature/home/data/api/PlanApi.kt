package com.yusufteker.pulse.feature.home.data.api

import com.yusufteker.pulse.shared.api.CreatePlanRoomRequest
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.AutoScheduleRequest
import com.yusufteker.pulse.shared.api.InviteUserRequest
import com.yusufteker.pulse.shared.api.PlanRoomDto
import com.yusufteker.pulse.shared.api.RespondToInviteRequest
import com.yusufteker.pulse.shared.api.TaskDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.request.setBody

class PlanApi(private val httpClient: HttpClient) {

    // --- TASK ENDPOINTS ---

    suspend fun createTask(request: CreateTaskRequest): TaskDto {
        return httpClient.post("tasks") {
            setBody(request)
        }.body()
    }

    suspend fun updateTask(taskId: String, request: CreateTaskRequest) {
        httpClient.put("tasks/$taskId") {
            setBody(request)
        }
    }

    suspend fun deleteTask(taskId: String) {
        httpClient.delete("tasks/$taskId")
    }

    suspend fun getMyTasks(fromTime: Long? = null, toTime: Long? = null): List<TaskDto> {
        return httpClient.get("tasks") {
            url {
                if (fromTime != null) parameters.append("from", fromTime.toString())
                if (toTime != null) parameters.append("to", toTime.toString())
            }
        }.body()
    }

    suspend fun getRoomTasks(roomId: String, fromTime: Long? = null, toTime: Long? = null): List<TaskDto> {
        return httpClient.get("rooms/$roomId/tasks") {
            url {
                if (fromTime != null) parameters.append("from", fromTime.toString())
                if (toTime != null) parameters.append("to", toTime.toString())
            }
        }.body()
    }
    
    suspend fun autoScheduleTasks(request: AutoScheduleRequest) {
        httpClient.post("tasks/auto-schedule") {
            setBody(request)
        }
    }

    ///tasks/123/join?roomId=room45
    suspend fun joinTask(taskId: String, roomId: String) {
        httpClient.post("tasks/$taskId/join") {
            url {
                parameters.append("roomId", roomId)
            }
        }
    }

    // --- PLAN ROOM ENDPOINTS ---

    suspend fun getMyRooms(): List<PlanRoomDto> {
        return httpClient.get("rooms").body()
    }

    suspend fun createPlanRoom(request: CreatePlanRoomRequest): PlanRoomDto {
        return httpClient.post("rooms") {
            setBody(request)
        }.body()
    }

    suspend fun inviteUserToRoom(roomId: String, request: InviteUserRequest) {
        httpClient.post("rooms/$roomId/invite") {
            setBody(request)
        }
    }
    
    suspend fun renameRoom(roomId: String, request: com.yusufteker.pulse.shared.api.RenamePlanRoomRequest) {
        httpClient.put("rooms/$roomId") {
            setBody(request)
        }
    }
    
    suspend fun deleteRoom(roomId: String) {
        httpClient.delete("rooms/$roomId")
    }

    suspend fun getMyPendingInvitations(): List<PlanRoomDto> {
        return httpClient.get("rooms/invitations").body()
    }

    suspend fun respondToInvite(roomId: String, accept: Boolean) {
        httpClient.post("rooms/$roomId/invitations/respond") {
            setBody(RespondToInviteRequest(accept))
        }
    }
}
