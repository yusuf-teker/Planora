package com.yusufteker.planora.feature.home.data.api

import com.yusufteker.planora.shared.api.CalendarAccessGrantDto
import com.yusufteker.planora.shared.api.CalendarAccessRequestDto
import com.yusufteker.planora.shared.api.TaskDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter

class CalendarApi(private val httpClient: HttpClient) {

    suspend fun requestCalendarAccess(userId: Int): Result<Unit> {
        return try {
            httpClient.post("calendar/request/$userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCalendarAccessRequests(): Result<List<CalendarAccessRequestDto>> {
        return try {
            val response: List<CalendarAccessRequestDto> = httpClient.get("calendar/requests").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptCalendarRequest(requestId: Int): Result<Unit> {
        return try {
            httpClient.post("calendar/requests/$requestId/accept")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectCalendarRequest(requestId: Int): Result<Unit> {
        return try {
            httpClient.post("calendar/requests/$requestId/reject")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAccessibleUsers(): Result<List<CalendarAccessGrantDto>> {
        return try {
            val response: List<CalendarAccessGrantDto> = httpClient.get("calendar/accessible").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCalendarGrants(): Result<List<CalendarAccessGrantDto>> {
        return try {
            val response: List<CalendarAccessGrantDto> = httpClient.get("calendar/grants").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeCalendarAccess(userId: Int): Result<Unit> {
        return try {
            httpClient.post("calendar/revoke/$userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSharedTasks(userId: Int, from: Long?, to: Long?): Result<List<TaskDto>> {
        return try {
            val response: List<TaskDto> = httpClient.get("calendar/tasks/$userId") {
                url {
                    if (from != null) parameter("from", from)
                    if (to != null) parameter("to", to)
                }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
