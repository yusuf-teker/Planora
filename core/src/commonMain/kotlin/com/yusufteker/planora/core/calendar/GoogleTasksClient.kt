package com.yusufteker.planora.core.calendar

import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskType
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class GoogleTaskListResponse(
    val items: List<GoogleTaskListItem>? = null
)

@Serializable
internal data class GoogleTaskListItem(
    val id: String,
    val title: String? = null
)

@Serializable
internal data class GoogleTasksResponse(
    val items: List<GoogleTaskItem>? = null
)

@Serializable
internal data class GoogleTaskItem(
    val id: String,
    val title: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val due: String? = null,
    val updated: String? = null
)

@Serializable
internal data class GoogleCalendarEventsResponse(
    val items: List<GoogleCalendarEventItem>? = null
)

@Serializable
internal data class GoogleCalendarEventItem(
    val id: String,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val start: GoogleCalendarEventTime? = null,
    val end: GoogleCalendarEventTime? = null,
    val status: String? = null
)

@Serializable
internal data class GoogleCalendarEventTime(
    val dateTime: String? = null,
    val date: String? = null
)

/**
 * Shared multiplatform client for performing authenticated queries to the Google Tasks and Google Calendar REST APIs.
 */
internal object GoogleTasksClient {

    /**
     * Retrieves both Google Calendar events and Google Tasks pending items for the authenticated user.
     *
     * @param accessToken Valid OAuth 2.0 access token with tasks.readonly and calendar.events.readonly scopes.
     * @param accountEmail The authenticated user's email address.
     * @return Combined list of [CalendarImportItem] instances representing events and tasks.
     */
    suspend fun fetchAllGoogleData(accessToken: String, accountEmail: String): List<CalendarImportItem> {
        val resultList = mutableListOf<CalendarImportItem>()
        var errorCount = 0
        var lastException: Exception? = null

        // 1. Fetch Calendar Events
        try {
            val events = fetchCalendarEvents(accessToken, accountEmail)
            resultList.addAll(events)
        } catch (e: Exception) {
            errorCount++
            lastException = e
        }

        // 2. Fetch Tasks
        try {
            val tasks = fetchTasks(accessToken, accountEmail)
            resultList.addAll(tasks)
        } catch (e: Exception) {
            errorCount++
            lastException = e
        }

        // If both failed, throw the last exception
        if (errorCount == 2 && lastException != null) {
            throw lastException
        }

        return resultList
    }

    /**
     * Retrieves primary Google Calendar events within past 30 days and future 30 days.
     */
    suspend fun fetchCalendarEvents(accessToken: String, accountEmail: String): List<CalendarImportItem> {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        val resultList = mutableListOf<CalendarImportItem>()
        try {
            val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
            val past30DaysMs = nowMs - (30L * 24 * 3600 * 1000L)
            val future30DaysMs = nowMs + (30L * 24 * 3600 * 1000L)
            val timeMinIso = Instant.fromEpochMilliseconds(past30DaysMs).toString()
            val timeMaxIso = Instant.fromEpochMilliseconds(future30DaysMs).toString()

            val calendarUrl = "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
                    "?timeMin=$timeMinIso&timeMax=$timeMaxIso&singleEvents=true&maxResults=250"

            val response = client.get(calendarUrl) {
                header("Authorization", "Bearer $accessToken")
            }
            val rawBody = response.bodyAsText()

            if (!response.status.isSuccess()) {
                throw Exception("Google Calendar API hatası (${response.status.value}): $rawBody")
            }

            val calendarResponse = Json { ignoreUnknownKeys = true; isLenient = true }
                .decodeFromString<GoogleCalendarEventsResponse>(rawBody)

            val items = calendarResponse.items ?: emptyList()
            val holidayKeywords = listOf("holiday", "tatil", "bayram", "birthday", "doğum günü")

            for (item in items) {
                if (item.status == "cancelled") continue
                val summary = item.summary?.trim()
                if (summary.isNullOrBlank()) continue

                val lowerSummary = summary.lowercase()
                if (holidayKeywords.any { lowerSummary.contains(it) }) continue

                val isAllDay = item.start?.dateTime == null && item.start?.date != null
                val startMs = item.start?.dateTime?.let {
                    try { Instant.parse(it).toEpochMilliseconds() } catch (_: Exception) { null }
                } ?: item.start?.date?.let {
                    try { Instant.parse("${it}T00:00:00Z").toEpochMilliseconds() } catch (_: Exception) { null }
                } ?: nowMs

                val endMs = item.end?.dateTime?.let {
                    try { Instant.parse(it).toEpochMilliseconds() } catch (_: Exception) { null }
                } ?: item.end?.date?.let {
                    try { Instant.parse("${it}T23:59:59Z").toEpochMilliseconds() } catch (_: Exception) { null }
                } ?: (startMs + 3600000L)

                resultList.add(
                    CalendarImportItem(
                        id = "gcal_${item.id}",
                        title = summary,
                        description = item.description?.trim()?.ifBlank { null },
                        location = item.location?.trim()?.ifBlank { null },
                        startTimeEpochMillis = startMs,
                        endTimeEpochMillis = endMs,
                        isAllDay = isAllDay,
                        calendarName = "Google Calendar",
                        accountName = "Google ($accountEmail)",
                        targetType = TaskType.EVENT,
                        priority = TaskPriority.MEDIUM,
                        isSelected = true
                    )
                )
            }
        } finally {
            client.close()
        }

        return resultList
    }

    /**
     * Retrieves all pending tasks across all user task lists from Google Tasks REST API.
     */
    suspend fun fetchTasks(accessToken: String, accountEmail: String): List<CalendarImportItem> {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        val resultList = mutableListOf<CalendarImportItem>()
        try {
            val listsUrl = "https://tasks.googleapis.com/tasks/v1/users/@me/lists"
            val response = client.get(listsUrl) {
                header("Authorization", "Bearer $accessToken")
            }
            val rawBody = response.bodyAsText()

            if (!response.status.isSuccess()) {
                throw Exception("Google Tasks API hatası (${response.status.value}): $rawBody")
            }

            val listResponse = Json { ignoreUnknownKeys = true; isLenient = true }
                .decodeFromString<GoogleTaskListResponse>(rawBody)

            val lists = listResponse.items ?: emptyList()
            val now = com.yusufteker.planora.core.utils.getCurrentTimeMs()

            for (tl in lists) {
                val listTitle = tl.title?.trim()?.ifBlank { "Google Tasks" } ?: "Google Tasks"
                val tasksUrl = "https://tasks.googleapis.com/tasks/v1/lists/${tl.id}/tasks?showCompleted=false&showHidden=false"
                val taskResp = client.get(tasksUrl) {
                    header("Authorization", "Bearer $accessToken")
                }
                val taskRawBody = taskResp.bodyAsText()

                if (!taskResp.status.isSuccess()) {
                    continue
                }

                val tasksResponse = Json { ignoreUnknownKeys = true; isLenient = true }
                    .decodeFromString<GoogleTasksResponse>(taskRawBody)

                val taskItems = tasksResponse.items ?: emptyList()

                for (t in taskItems) {
                    if (t.title.isNullOrBlank()) continue
                    val dueEpoch = t.due?.let {
                        try {
                            Instant.parse(it).toEpochMilliseconds()
                        } catch (_: Exception) {
                            null
                        }
                    } ?: now

                    resultList.add(
                        CalendarImportItem(
                            id = "gtask_${t.id}",
                            title = t.title.trim(),
                            description = t.notes?.trim()?.ifBlank { null },
                            location = null,
                            startTimeEpochMillis = dueEpoch,
                            endTimeEpochMillis = dueEpoch + 3600000L,
                            isAllDay = true,
                            calendarName = listTitle,
                            accountName = "Google Tasks ($accountEmail)",
                            targetType = TaskType.TASK,
                            priority = TaskPriority.MEDIUM,
                            isSelected = true
                        )
                    )
                }
            }
        } finally {
            client.close()
        }

        return resultList
    }
}
