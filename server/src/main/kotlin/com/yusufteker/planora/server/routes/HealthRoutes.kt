package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.*
import com.yusufteker.planora.server.plugins.AI_RATE_LIMIT
import com.yusufteker.planora.server.service.FcmService
import com.yusufteker.planora.server.service.HealthAiQuotaDto
import com.yusufteker.planora.server.service.HealthAiQuotaExceededException
import com.yusufteker.planora.server.service.HealthAiService
import com.yusufteker.planora.server.service.HealthLeaderboardService
import com.yusufteker.planora.server.service.HuaweiAuthUrlResponse
import com.yusufteker.planora.server.service.HuaweiHealthService
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Serializable
data class HealthCheckResponse(
    val status: String = "UP",
    val service: String = "HealthTrack",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class FoodAiRequest(
    val text: String,
    val date: String,
    val imageBase64: String? = null,
    val imageMimeType: String? = "image/jpeg"
)

@Serializable
data class WorkoutAiRequest(
    val text: String,
    val userWeightKg: Double? = null,
    val date: String
)

@Serializable
data class HealthAiErrorResponse(
    val error: String,
    val code: String = "AI_ERROR",
    val quota: HealthAiQuotaDto? = null
)

// Sync DTOs
@Serializable
data class FoodLogSyncDto(
    val id: String,
    val date: String,
    val rawText: String,
    val summary: String? = null,
    val totalCalories: Int = 0,
    val itemsJson: String = "[]",
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class NutritionGoalSyncDto(
    val id: String,
    val calories: Int = 2000,
    val proteinG: Double = 80.0,
    val carbsG: Double = 250.0,
    val fatG: Double = 65.0,
    val fiberG: Double = 30.0,
    val vitaminCMg: Double = 90.0,
    val vitaminDUg: Double = 15.0,
    val vitaminB12Ug: Double = 2.4,
    val ironMg: Double = 14.0,
    val calciumMg: Double = 1000.0,
    val magnesiumMg: Double = 350.0,
    val fitnessGoal: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class WorkoutSyncDto(
    val id: String,
    val date: String,
    val rawText: String,
    val caloriesBurned: Int = 0,
    val durationMinutes: Int = 0,
    val targetCalories: Int = 300,
    val targetDurationMinutes: Int = 45,
    val aiAnalysis: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class WeightSyncDto(
    val id: String,
    val date: String,
    val weightKg: Double,
    val targetWeightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val muscleMassKg: Double? = null,
    val musclePercent: Double? = null,
    val note: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class AppointmentSyncDto(
    val id: String,
    val dateTime: Long,
    val doctorName: String,
    val hospitalName: String,
    val department: String? = null,
    val notes: String? = null,
    val reminderMinutes: Int = 60,
    val planoraTaskId: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class HealthSyncPushRequest(
    val foods: List<FoodLogSyncDto> = emptyList(),
    val nutritionGoals: NutritionGoalSyncDto? = null,
    val workouts: List<WorkoutSyncDto> = emptyList(),
    val weights: List<WeightSyncDto> = emptyList(),
    val appointments: List<AppointmentSyncDto> = emptyList()
)

@Serializable
data class HealthSyncPushResponse(
    val syncedFoodIds: List<String> = emptyList(),
    val syncedNutritionGoalId: String? = null,
    val syncedWorkoutIds: List<String> = emptyList(),
    val syncedWeightIds: List<String> = emptyList(),
    val syncedAppointmentIds: List<String> = emptyList(),
    val serverTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class HealthSyncPullResponse(
    val foods: List<FoodLogSyncDto> = emptyList(),
    val nutritionGoals: NutritionGoalSyncDto? = null,
    val workouts: List<WorkoutSyncDto> = emptyList(),
    val weights: List<WeightSyncDto> = emptyList(),
    val appointments: List<AppointmentSyncDto> = emptyList(),
    val serverTimestamp: Long = System.currentTimeMillis()
)

fun Route.healthRoutes() {
    // 1. Unauthenticated health checks for Uptime Robot & monitoring
    route("/health") {
        get {
            call.respond(HttpStatusCode.OK, HealthCheckResponse())
        }
        head {
            call.respond(HttpStatusCode.OK)
        }

        get("/db") {
            val isDbHealthy = try {
                dbQuery {
                    UsersTable.selectAll().limit(1).count() >= 0
                }
            } catch (e: Exception) {
                false
            }
            if (isDbHealthy) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP", "database" to "CONNECTED"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "DOWN", "database" to "DISCONNECTED"))
            }
        }

        get("/huawei/callback") {
            val error = call.request.queryParameters["error"]
            val errorDescription = call.request.queryParameters["error_description"]
            val code = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]

            val (isSuccess, statusParam, reasonText) = if (!error.isNullOrBlank()) {
                Triple(false, "error", errorDescription?.takeIf { it.isNotBlank() } ?: error)
            } else {
                val res = HuaweiHealthService.handleOAuthCallback(code = code, state = state)
                if (res.success) {
                    Triple(true, "connected", null)
                } else {
                    Triple(false, "error", res.errorMessage ?: res.errorCode ?: "callback_failed")
                }
            }

            val encodedReason = reasonText?.let {
                URLEncoder.encode(it, StandardCharsets.UTF_8).replace("+", "%20")
            }
            val deepLinkUrl = if (isSuccess) {
                "healthtrack://huawei/sync-done?status=$statusParam"
            } else {
                "healthtrack://huawei/sync-done?status=$statusParam&reason=${encodedReason.orEmpty()}"
            }

            val html = buildHuaweiCallbackHtml(
                isSuccess = isSuccess,
                deepLinkUrl = deepLinkUrl,
                reasonText = reasonText
            )
            call.respondText(html, ContentType.Text.Html.withCharset(Charsets.UTF_8), HttpStatusCode.OK)
        }
    }

    // 2. Authenticated HealthTrack APIs
    authenticate("auth-jwt") {
        route("/health") {

            // --- AI Endpoints ---
            get("/ai/quota") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val quota = HealthAiService.getUserQuota(userId)
                call.respond(HttpStatusCode.OK, quota)
            }

            rateLimit(AI_RATE_LIMIT) {
                post("/ai/food") {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val req = call.receive<FoodAiRequest>()
                    val customApiKey = call.request.headers["X-Gemini-Api-Key"]?.trim()?.takeIf { it.isNotBlank() }
                    try {
                        val result = HealthAiService.analyzeFood(
                            userId = userId,
                            rawText = req.text,
                            date = req.date,
                            imageBase64 = req.imageBase64,
                            imageMimeType = req.imageMimeType,
                            customApiKey = customApiKey
                        )
                        call.respond(HttpStatusCode.OK, result)
                    } catch (e: HealthAiQuotaExceededException) {
                        call.respond(
                            HttpStatusCode.TooManyRequests,
                            HealthAiErrorResponse(
                                error = e.message,
                                code = "QUOTA_EXCEEDED",
                                quota = e.quota
                            )
                        )
                    } catch (e: Exception) {
                        println("[HealthRoutes] Error in /health/ai/food: ${e.message}")
                        e.printStackTrace()
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "AI food analysis failed")))
                    }
                }

                post("/ai/workout") {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val req = call.receive<WorkoutAiRequest>()
                    val customApiKey = call.request.headers["X-Gemini-Api-Key"]?.trim()?.takeIf { it.isNotBlank() }
                    try {
                        val result = HealthAiService.analyzeWorkout(
                            userId = userId,
                            rawText = req.text,
                            userWeightKg = req.userWeightKg,
                            date = req.date,
                            customApiKey = customApiKey
                        )
                        call.respond(HttpStatusCode.OK, result)
                    } catch (e: HealthAiQuotaExceededException) {
                        call.respond(
                            HttpStatusCode.TooManyRequests,
                            HealthAiErrorResponse(
                                error = e.message,
                                code = "QUOTA_EXCEEDED",
                                quota = e.quota
                            )
                        )
                    } catch (e: Exception) {
                        println("[HealthRoutes] Error in /health/ai/workout: ${e.message}")
                        e.printStackTrace()
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "AI workout analysis failed")))
                    }
                }
            }

            // --- Nutrition Goals Direct Access ---
            get("/nutrition/goals") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val goal = dbQuery {
                    HealthNutritionGoalsTable.selectAll()
                        .where { (HealthNutritionGoalsTable.userId eq userId) and (HealthNutritionGoalsTable.isDeleted eq false) }
                        .map {
                            NutritionGoalSyncDto(
                                id = it[HealthNutritionGoalsTable.id].value,
                                calories = it[HealthNutritionGoalsTable.calories],
                                proteinG = it[HealthNutritionGoalsTable.proteinG],
                                carbsG = it[HealthNutritionGoalsTable.carbsG],
                                fatG = it[HealthNutritionGoalsTable.fatG],
                                fiberG = it[HealthNutritionGoalsTable.fiberG],
                                vitaminCMg = it[HealthNutritionGoalsTable.vitaminCMg],
                                vitaminDUg = it[HealthNutritionGoalsTable.vitaminDUg],
                                vitaminB12Ug = it[HealthNutritionGoalsTable.vitaminB12Ug],
                                ironMg = it[HealthNutritionGoalsTable.ironMg],
                                calciumMg = it[HealthNutritionGoalsTable.calciumMg],
                                magnesiumMg = it[HealthNutritionGoalsTable.magnesiumMg],
                                fitnessGoal = it[HealthNutritionGoalsTable.fitnessGoal],
                                updatedAt = it[HealthNutritionGoalsTable.updatedAt]
                            )
                        }.firstOrNull()
                }

                if (goal != null) {
                    call.respond(HttpStatusCode.OK, goal)
                } else {
                    call.respond(HttpStatusCode.OK, NutritionGoalSyncDto(id = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis()))
                }
            }

            // --- Sync Push ---
            post("/sync/push") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val req = call.receive<HealthSyncPushRequest>()
                val syncedFoodIds = mutableListOf<String>()
                var syncedGoalId: String? = null
                val syncedWorkoutIds = mutableListOf<String>()
                val syncedWeightIds = mutableListOf<String>()
                val syncedAppointmentIds = mutableListOf<String>()

                val recentPushCutoffMs = System.currentTimeMillis() - 60 * 60 * 1000L
                val newWorkoutsForPush = mutableListOf<WorkoutSyncDto>()
                var actorName = ""
                var actorUsername = ""
                var followerIds = emptyList<Int>()

                dbQuery {
                    // 1. Process Foods
                    req.foods.forEach { food ->
                        val existing = HealthFoodLogsTable.selectAll()
                            .where { HealthFoodLogsTable.id eq food.id }
                            .firstOrNull()

                        if (existing == null) {
                            HealthFoodLogsTable.insert {
                                it[id] = food.id
                                it[HealthFoodLogsTable.userId] = userId
                                it[date] = food.date
                                it[rawText] = food.rawText
                                it[summary] = food.summary
                                it[totalCalories] = food.totalCalories
                                it[itemsJson] = food.itemsJson
                                it[createdAt] = Instant.now()
                                it[updatedAt] = food.updatedAt
                                it[isDeleted] = food.isDeleted
                            }
                            syncedFoodIds.add(food.id)
                        } else if (existing[HealthFoodLogsTable.userId].value != userId) {
                            // Ownership guard: do not overwrite or crash on another user's record
                        } else if (food.updatedAt > existing[HealthFoodLogsTable.updatedAt]) {
                            HealthFoodLogsTable.update({ (HealthFoodLogsTable.id eq food.id) and (HealthFoodLogsTable.userId eq userId) }) {
                                it[date] = food.date
                                it[rawText] = food.rawText
                                it[summary] = food.summary
                                it[totalCalories] = food.totalCalories
                                it[itemsJson] = food.itemsJson
                                it[updatedAt] = food.updatedAt
                                it[isDeleted] = food.isDeleted
                            }
                            syncedFoodIds.add(food.id)
                        } else {
                            syncedFoodIds.add(food.id) // Already up-to-date on server
                        }
                    }

                    // 2. Process Nutrition Goals
                    req.nutritionGoals?.let { goal ->
                        val existing = HealthNutritionGoalsTable.selectAll()
                            .where { HealthNutritionGoalsTable.userId eq userId }
                            .firstOrNull()

                        if (existing == null) {
                            val idTaken = HealthNutritionGoalsTable.selectAll()
                                .where { HealthNutritionGoalsTable.id eq goal.id }
                                .count() > 0
                            val safeGoalId = if (idTaken) UUID.randomUUID().toString() else goal.id
                            HealthNutritionGoalsTable.insert {
                                it[id] = safeGoalId
                                it[HealthNutritionGoalsTable.userId] = userId
                                it[calories] = goal.calories
                                it[proteinG] = goal.proteinG
                                it[carbsG] = goal.carbsG
                                it[fatG] = goal.fatG
                                it[fiberG] = goal.fiberG
                                it[vitaminCMg] = goal.vitaminCMg
                                it[vitaminDUg] = goal.vitaminDUg
                                it[vitaminB12Ug] = goal.vitaminB12Ug
                                it[ironMg] = goal.ironMg
                                it[calciumMg] = goal.calciumMg
                                it[magnesiumMg] = goal.magnesiumMg
                                it[fitnessGoal] = goal.fitnessGoal
                                it[updatedAt] = goal.updatedAt
                                it[isDeleted] = goal.isDeleted
                            }
                            syncedGoalId = safeGoalId
                        } else if (goal.updatedAt > existing[HealthNutritionGoalsTable.updatedAt]) {
                            HealthNutritionGoalsTable.update({ HealthNutritionGoalsTable.userId eq userId }) {
                                it[calories] = goal.calories
                                it[proteinG] = goal.proteinG
                                it[carbsG] = goal.carbsG
                                it[fatG] = goal.fatG
                                it[fiberG] = goal.fiberG
                                it[vitaminCMg] = goal.vitaminCMg
                                it[vitaminDUg] = goal.vitaminDUg
                                it[vitaminB12Ug] = goal.vitaminB12Ug
                                it[ironMg] = goal.ironMg
                                it[calciumMg] = goal.calciumMg
                                it[magnesiumMg] = goal.magnesiumMg
                                it[fitnessGoal] = goal.fitnessGoal
                                it[updatedAt] = goal.updatedAt
                                it[isDeleted] = goal.isDeleted
                            }
                            syncedGoalId = goal.id
                        } else {
                            syncedGoalId = goal.id
                        }
                    }

                    // 3. Process Workouts
                    req.workouts.forEach { w ->
                        val existing = HealthWorkoutsTable.selectAll()
                            .where { HealthWorkoutsTable.id eq w.id }
                            .firstOrNull()

                        val isStepSummary = w.id.endsWith("_steps") ||
                            w.rawText.contains("Adım • Günlük Hareket", ignoreCase = true) ||
                            w.rawText.contains("Steps • Daily Movement", ignoreCase = true)

                        if (existing == null) {
                            HealthWorkoutsTable.insert {
                                it[id] = w.id
                                it[HealthWorkoutsTable.userId] = userId
                                it[date] = w.date
                                it[rawText] = w.rawText
                                it[caloriesBurned] = w.caloriesBurned
                                it[durationMinutes] = w.durationMinutes
                                it[targetCalories] = w.targetCalories
                                it[targetDurationMinutes] = w.targetDurationMinutes
                                it[aiAnalysis] = w.aiAnalysis
                                it[createdAt] = Instant.now()
                                it[updatedAt] = w.updatedAt
                                it[isDeleted] = w.isDeleted
                            }
                            if (!w.isDeleted && !isStepSummary && (w.caloriesBurned > 0 || w.durationMinutes > 0) && w.updatedAt >= recentPushCutoffMs) {
                                newWorkoutsForPush.add(w)
                            }
                            syncedWorkoutIds.add(w.id)
                        } else if (existing[HealthWorkoutsTable.userId].value != userId) {
                            // Ownership guard
                        } else if (w.updatedAt > existing[HealthWorkoutsTable.updatedAt]) {
                            HealthWorkoutsTable.update({ (HealthWorkoutsTable.id eq w.id) and (HealthWorkoutsTable.userId eq userId) }) {
                                it[date] = w.date
                                it[rawText] = w.rawText
                                it[caloriesBurned] = w.caloriesBurned
                                it[durationMinutes] = w.durationMinutes
                                it[targetCalories] = w.targetCalories
                                it[targetDurationMinutes] = w.targetDurationMinutes
                                it[aiAnalysis] = w.aiAnalysis
                                it[updatedAt] = w.updatedAt
                                it[isDeleted] = w.isDeleted
                            }
                            val wasUnlogged = existing[HealthWorkoutsTable.caloriesBurned] == 0 &&
                                existing[HealthWorkoutsTable.durationMinutes] == 0
                            if (wasUnlogged && !w.isDeleted && !isStepSummary && (w.caloriesBurned > 0 || w.durationMinutes > 0) && w.updatedAt >= recentPushCutoffMs) {
                                newWorkoutsForPush.add(w)
                            }
                            syncedWorkoutIds.add(w.id)
                        } else {
                            syncedWorkoutIds.add(w.id)
                        }
                    }

                    // 4. Process Weights
                    req.weights.forEach { wt ->
                        val existing = HealthWeightsTable.selectAll()
                            .where { HealthWeightsTable.id eq wt.id }
                            .firstOrNull()

                        if (existing == null) {
                            HealthWeightsTable.insert {
                                it[id] = wt.id
                                it[HealthWeightsTable.userId] = userId
                                it[date] = wt.date
                                it[weightKg] = wt.weightKg
                                it[targetWeightKg] = wt.targetWeightKg
                                it[bodyFatPercent] = if (wt.isDeleted) null else wt.bodyFatPercent
                                it[muscleMassKg] = if (wt.isDeleted) null else wt.muscleMassKg
                                it[musclePercent] = if (wt.isDeleted) null else wt.musclePercent
                                it[note] = wt.note
                                it[createdAt] = Instant.now()
                                it[updatedAt] = wt.updatedAt
                                it[isDeleted] = wt.isDeleted
                            }
                            syncedWeightIds.add(wt.id)
                        } else if (existing[HealthWeightsTable.userId].value != userId) {
                            // Ownership guard
                        } else if (wt.updatedAt > existing[HealthWeightsTable.updatedAt]) {
                            HealthWeightsTable.update({ (HealthWeightsTable.id eq wt.id) and (HealthWeightsTable.userId eq userId) }) {
                                it[date] = wt.date
                                it[weightKg] = wt.weightKg
                                it[targetWeightKg] = wt.targetWeightKg
                                it[bodyFatPercent] = if (wt.isDeleted) null else wt.bodyFatPercent
                                it[muscleMassKg] = if (wt.isDeleted) null else wt.muscleMassKg
                                it[musclePercent] = if (wt.isDeleted) null else wt.musclePercent
                                it[note] = wt.note
                                it[updatedAt] = wt.updatedAt
                                it[isDeleted] = wt.isDeleted
                            }
                            syncedWeightIds.add(wt.id)
                        } else {
                            syncedWeightIds.add(wt.id)
                        }
                    }

                    // 5. Process Appointments & Mirror into Planora Tasks
                    req.appointments.forEach { appt ->
                        val existing = HealthAppointmentsTable.selectAll()
                            .where { HealthAppointmentsTable.id eq appt.id }
                            .firstOrNull()

                        if (existing != null && existing[HealthAppointmentsTable.userId].value != userId) {
                            return@forEach // Ownership guard
                        }

                        var planoraTaskId = existing?.get(HealthAppointmentsTable.planoraTaskId)?.value ?: appt.planoraTaskId

                        if (appt.isDeleted) {
                            // If deleted, remove mirrored task if owned by user
                            existing?.get(HealthAppointmentsTable.planoraTaskId)?.value?.let { pTaskId ->
                                TasksTable.deleteWhere { (TasksTable.id eq pTaskId) and (TasksTable.creatorId eq userId) }
                            }
                            if (existing != null) {
                                HealthAppointmentsTable.update({ (HealthAppointmentsTable.id eq appt.id) and (HealthAppointmentsTable.userId eq userId) }) {
                                    it[isDeleted] = true
                                    it[updatedAt] = appt.updatedAt
                                    it[HealthAppointmentsTable.planoraTaskId] = null
                                }
                            }
                            syncedAppointmentIds.add(appt.id)
                            return@forEach
                        }

                        // Mirror or update Planora task (EVENT) — strictly verify ownership
                        val taskTitle = "Randevu: ${appt.doctorName} - ${appt.hospitalName}"
                        val taskDesc = appt.notes ?: "Sağlık Randevusu"
                        val eventStart = appt.dateTime
                        val eventEnd = appt.dateTime + (30 * 60 * 1000L) // 30 min duration default

                        val ownsTask = planoraTaskId != null && TasksTable.selectAll()
                            .where { (TasksTable.id eq planoraTaskId) and (TasksTable.creatorId eq userId) }
                            .count() > 0

                        if (ownsTask && planoraTaskId != null) {
                            // Update existing owned Planora task
                            TasksTable.update({ (TasksTable.id eq planoraTaskId) and (TasksTable.creatorId eq userId) }) {
                                it[title] = taskTitle
                                it[description] = taskDesc
                                it[startTime] = eventStart
                                it[endTime] = eventEnd
                                it[color] = "#E91E63"
                            }
                        } else {
                            // Create new Planora event
                            val newTaskId = UUID.randomUUID().toString()
                            TaskEntity.new(newTaskId) {
                                this.creator = UserEntity[userId]
                                this.title = taskTitle
                                this.description = taskDesc
                                this.startTime = eventStart
                                this.endTime = eventEnd
                                this.type = TaskType.EVENT
                                this.status = TaskStatus.PENDING
                                this.visibility = TaskVisibility.PRIVATE
                                this.tags = Json.encodeToString(listOf("health", "appointment"))
                                this.color = "#E91E63"
                                this.reminders = Json.encodeToString(listOf(appt.reminderMinutes))
                            }
                            planoraTaskId = newTaskId
                        }

                        if (existing == null) {
                            HealthAppointmentsTable.insert {
                                it[id] = appt.id
                                it[HealthAppointmentsTable.userId] = userId
                                it[dateTime] = appt.dateTime
                                it[doctorName] = appt.doctorName
                                it[hospitalName] = appt.hospitalName
                                it[department] = appt.department
                                it[notes] = appt.notes
                                it[reminderMinutes] = appt.reminderMinutes
                                it[HealthAppointmentsTable.planoraTaskId] = planoraTaskId
                                it[createdAt] = Instant.now()
                                it[updatedAt] = appt.updatedAt
                                it[isDeleted] = appt.isDeleted
                            }
                        } else if (appt.updatedAt > existing[HealthAppointmentsTable.updatedAt]) {
                            HealthAppointmentsTable.update({ (HealthAppointmentsTable.id eq appt.id) and (HealthAppointmentsTable.userId eq userId) }) {
                                it[dateTime] = appt.dateTime
                                it[doctorName] = appt.doctorName
                                it[hospitalName] = appt.hospitalName
                                it[department] = appt.department
                                it[notes] = appt.notes
                                it[reminderMinutes] = appt.reminderMinutes
                                it[HealthAppointmentsTable.planoraTaskId] = planoraTaskId
                                it[updatedAt] = appt.updatedAt
                                it[isDeleted] = appt.isDeleted
                            }
                        }
                        syncedAppointmentIds.add(appt.id)
                    }

                    if (newWorkoutsForPush.isNotEmpty()) {
                        val actor = UserEntity.findById(userId)
                        actorName = actor?.name?.takeIf { it.isNotBlank() } ?: actor?.username.orEmpty()
                        actorUsername = actor?.username.orEmpty()
                        val allFollowers = FollowerEntity.find { FollowersTable.followedId eq userId }
                            .map { it.follower.id.value }
                            .filter { it != userId }
                            .distinct()

                        val mutedFollowers = if (allFollowers.isNotEmpty()) {
                            HealthWorkoutNotificationMutesTable
                                .selectAll()
                                .where {
                                    (HealthWorkoutNotificationMutesTable.mutedUserId eq userId) and
                                    (HealthWorkoutNotificationMutesTable.userId inList allFollowers)
                                }
                                .map { it[HealthWorkoutNotificationMutesTable.userId].value }
                                .toSet()
                        } else emptySet()

                        followerIds = allFollowers.filter { it !in mutedFollowers }
                    }
                }

                if (followerIds.isNotEmpty() && actorName.isNotBlank()) {
                    val app = call.application
                    val latestWorkout = newWorkoutsForPush.maxByOrNull { it.updatedAt }
                    app.launch {
                        if (latestWorkout != null) {
                            runCatching {
                                val nickname = actorName.trim()
                                    .substringBefore(" ")
                                    .ifBlank { actorUsername.ifBlank { "Arkadaşın" } }
                                val sport = detectWorkoutSportDisplay(latestWorkout.rawText)
                                val title = if (latestWorkout.durationMinutes > 0) {
                                    "${sport.emoji} $nickname ${latestWorkout.durationMinutes} dk ${sport.phraseTr}"
                                } else {
                                    "${sport.emoji} $nickname ${sport.phraseTr}"
                                }
                                val body = if (latestWorkout.caloriesBurned > 0) {
                                    "🔥 ${latestWorkout.caloriesBurned} kcal yaktı • Sen de serini koru!"
                                } else {
                                    "HealthTrack'te yeni antrenman kaydı!"
                                }
                                FcmService.sendPushToUsers(
                                    userIds = followerIds,
                                    title = title,
                                    body = body,
                                    data = mapOf(
                                        "title" to title,
                                        "body" to body,
                                        "activityType" to "WORKOUT",
                                        "sportType" to sport.sportKey,
                                        "actorUserId" to userId.toString(),
                                        "actorName" to nickname,
                                        "actorUsername" to actorUsername,
                                        "targetApp" to "HEALTHTRACK",
                                        "notificationId" to "push_workout_${userId}_${latestWorkout.id}"
                                    ),
                                    targetApp = "HEALTHTRACK"
                                )
                            }
                        }
                    }
                }

                call.respond(
                    HttpStatusCode.OK,
                    HealthSyncPushResponse(
                        syncedFoodIds = syncedFoodIds,
                        syncedNutritionGoalId = syncedGoalId,
                        syncedWorkoutIds = syncedWorkoutIds,
                        syncedWeightIds = syncedWeightIds,
                        syncedAppointmentIds = syncedAppointmentIds
                    )
                )
            }

            // --- Sync Pull ---
            get("/sync/pull") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L

                val pullData = dbQuery {
                    val foods = HealthFoodLogsTable.selectAll()
                        .where { (HealthFoodLogsTable.userId eq userId) and (HealthFoodLogsTable.updatedAt greaterEq since) }
                        .map {
                            FoodLogSyncDto(
                                id = it[HealthFoodLogsTable.id].value,
                                date = it[HealthFoodLogsTable.date],
                                rawText = it[HealthFoodLogsTable.rawText],
                                summary = it[HealthFoodLogsTable.summary],
                                totalCalories = it[HealthFoodLogsTable.totalCalories],
                                itemsJson = it[HealthFoodLogsTable.itemsJson],
                                updatedAt = it[HealthFoodLogsTable.updatedAt],
                                isDeleted = it[HealthFoodLogsTable.isDeleted]
                            )
                        }

                    val goal = HealthNutritionGoalsTable.selectAll()
                        .where { (HealthNutritionGoalsTable.userId eq userId) and (HealthNutritionGoalsTable.updatedAt greaterEq since) }
                        .map {
                            NutritionGoalSyncDto(
                                id = it[HealthNutritionGoalsTable.id].value,
                                calories = it[HealthNutritionGoalsTable.calories],
                                proteinG = it[HealthNutritionGoalsTable.proteinG],
                                carbsG = it[HealthNutritionGoalsTable.carbsG],
                                fatG = it[HealthNutritionGoalsTable.fatG],
                                fiberG = it[HealthNutritionGoalsTable.fiberG],
                                vitaminCMg = it[HealthNutritionGoalsTable.vitaminCMg],
                                vitaminDUg = it[HealthNutritionGoalsTable.vitaminDUg],
                                vitaminB12Ug = it[HealthNutritionGoalsTable.vitaminB12Ug],
                                ironMg = it[HealthNutritionGoalsTable.ironMg],
                                calciumMg = it[HealthNutritionGoalsTable.calciumMg],
                                magnesiumMg = it[HealthNutritionGoalsTable.magnesiumMg],
                                fitnessGoal = it[HealthNutritionGoalsTable.fitnessGoal],
                                updatedAt = it[HealthNutritionGoalsTable.updatedAt],
                                isDeleted = it[HealthNutritionGoalsTable.isDeleted]
                            )
                        }.firstOrNull()

                    val workouts = HealthWorkoutsTable.selectAll()
                        .where { (HealthWorkoutsTable.userId eq userId) and (HealthWorkoutsTable.updatedAt greaterEq since) }
                        .map {
                            WorkoutSyncDto(
                                id = it[HealthWorkoutsTable.id].value,
                                date = it[HealthWorkoutsTable.date],
                                rawText = it[HealthWorkoutsTable.rawText],
                                caloriesBurned = it[HealthWorkoutsTable.caloriesBurned],
                                durationMinutes = it[HealthWorkoutsTable.durationMinutes],
                                targetCalories = it[HealthWorkoutsTable.targetCalories],
                                targetDurationMinutes = it[HealthWorkoutsTable.targetDurationMinutes],
                                aiAnalysis = it[HealthWorkoutsTable.aiAnalysis],
                                updatedAt = it[HealthWorkoutsTable.updatedAt],
                                isDeleted = it[HealthWorkoutsTable.isDeleted]
                            )
                        }

                    val weights = HealthWeightsTable.selectAll()
                        .where { (HealthWeightsTable.userId eq userId) and (HealthWeightsTable.updatedAt greaterEq since) }
                        .map {
                            WeightSyncDto(
                                id = it[HealthWeightsTable.id].value,
                                date = it[HealthWeightsTable.date],
                                weightKg = it[HealthWeightsTable.weightKg],
                                targetWeightKg = it[HealthWeightsTable.targetWeightKg],
                                bodyFatPercent = it[HealthWeightsTable.bodyFatPercent],
                                muscleMassKg = it[HealthWeightsTable.muscleMassKg],
                                musclePercent = it[HealthWeightsTable.musclePercent],
                                note = it[HealthWeightsTable.note],
                                updatedAt = it[HealthWeightsTable.updatedAt],
                                isDeleted = it[HealthWeightsTable.isDeleted]
                            )
                        }

                    val appointments = HealthAppointmentsTable.selectAll()
                        .where { (HealthAppointmentsTable.userId eq userId) and (HealthAppointmentsTable.updatedAt greaterEq since) }
                        .map {
                            AppointmentSyncDto(
                                id = it[HealthAppointmentsTable.id].value,
                                dateTime = it[HealthAppointmentsTable.dateTime],
                                doctorName = it[HealthAppointmentsTable.doctorName],
                                hospitalName = it[HealthAppointmentsTable.hospitalName],
                                department = it[HealthAppointmentsTable.department],
                                notes = it[HealthAppointmentsTable.notes],
                                reminderMinutes = it[HealthAppointmentsTable.reminderMinutes],
                                planoraTaskId = it[HealthAppointmentsTable.planoraTaskId]?.value,
                                updatedAt = it[HealthAppointmentsTable.updatedAt],
                                isDeleted = it[HealthAppointmentsTable.isDeleted]
                            )
                        }

                    HealthSyncPullResponse(
                        foods = foods,
                        nutritionGoals = goal,
                        workouts = workouts,
                        weights = weights,
                        appointments = appointments
                    )
                }

                call.respond(HttpStatusCode.OK, pullData)
            }

            // --- Leaderboard (Global & Friends, Daily & Weekly) ---
            get("/leaderboard") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val period = call.request.queryParameters["period"] ?: "WEEKLY"
                val scope = call.request.queryParameters["scope"] ?: "GLOBAL"
                val category = call.request.queryParameters["category"] ?: "DISCIPLINE_XP"
                val dateIso = call.request.queryParameters["date"]

                val leaderboard = HealthLeaderboardService.getLeaderboard(
                    currentUserId = userId,
                    period = period,
                    scope = scope,
                    category = category,
                    dateIso = dateIso
                )
                call.respond(HttpStatusCode.OK, leaderboard)
            }

            // --- Friend Workout Notification Preferences ---
            get("/friends/muted-workouts") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val mutedIds = dbQuery {
                    HealthWorkoutNotificationMutesTable
                        .selectAll()
                        .where { HealthWorkoutNotificationMutesTable.userId eq userId }
                        .map { it[HealthWorkoutNotificationMutesTable.mutedUserId].value }
                }
                call.respond(HttpStatusCode.OK, mutedIds)
            }

            post("/friends/{targetUserId}/toggle-workout-notifications") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val targetUserId = call.parameters["targetUserId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid target user ID")

                if (userId == targetUserId) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Cannot mute yourself")
                }

                val isNowMuted = dbQuery {
                    val existing = HealthWorkoutNotificationMutesTable
                        .selectAll()
                        .where {
                            (HealthWorkoutNotificationMutesTable.userId eq userId) and
                            (HealthWorkoutNotificationMutesTable.mutedUserId eq targetUserId)
                        }
                        .firstOrNull()

                    if (existing != null) {
                        HealthWorkoutNotificationMutesTable.deleteWhere {
                            (HealthWorkoutNotificationMutesTable.userId eq userId) and
                            (HealthWorkoutNotificationMutesTable.mutedUserId eq targetUserId)
                        }
                        false
                    } else {
                        HealthWorkoutNotificationMutesTable.insert {
                            it[HealthWorkoutNotificationMutesTable.userId] = userId
                            it[HealthWorkoutNotificationMutesTable.mutedUserId] = targetUserId
                            it[createdAt] = Instant.now()
                        }
                        true
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf("targetUserId" to targetUserId, "isMuted" to isNowMuted))
            }

            // --- Huawei Health Service Kit Cloud Integration ---
            get("/huawei/status") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val status = HuaweiHealthService.getConnectionStatus(userId)
                call.respond(HttpStatusCode.OK, status)
            }

            get("/huawei/auth-url") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                if (!HuaweiHealthService.isConfigured()) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf("error" to "Huawei Health Kit sunucuda henüz yapılandırılmadı (HUAWEI_CLIENT_SECRET eksik).")
                    )
                }
                val authUrl = HuaweiHealthService.buildAuthorizationUrl(userId)
                call.respond(HttpStatusCode.OK, HuaweiAuthUrlResponse(authUrl = authUrl, configured = true))
            }

            get("/huawei/sync") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val days = call.request.queryParameters["days"]?.toIntOrNull()?.coerceIn(1, 7) ?: 7
                val tz = call.request.queryParameters["tz"]?.takeIf { it.isNotBlank() } ?: "Europe/Istanbul"
                val result = HuaweiHealthService.syncHealthData(userId = userId, days = days, timeZoneId = tz)
                call.respond(HttpStatusCode.OK, result)
            }

            delete("/huawei/disconnect") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                HuaweiHealthService.disconnect(userId)
                call.respond(HttpStatusCode.OK, mapOf("disconnected" to true))
            }
        }
    }
}

private fun escapeHtml(input: String): String = buildString(input.length) {
    for (ch in input) {
        when (ch) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(ch)
        }
    }
}

private fun buildHuaweiCallbackHtml(
    isSuccess: Boolean,
    deepLinkUrl: String,
    reasonText: String?
): String {
    val safeUrlAttr = escapeHtml(deepLinkUrl)
    val jsUrl = deepLinkUrl
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("<", "\\u003C")
    val badgeBg = if (isSuccess) "rgba(16, 185, 129, 0.16)" else "rgba(239, 68, 68, 0.16)"
    val badgeColor = if (isSuccess) "#34D399" else "#F87171"
    val badgeText = if (isSuccess) "Huawei Sağlık Bağlandı ✓" else "Bağlantı Tamamlanamadı"
    val title = if (isSuccess) "HealthTrack'e Dönülüyor..." else "Huawei Yetkilendirmesi Başarısız"
    val subtitle = if (isSuccess) {
        "Huawei Health Service Kit izni onaylandı. Uygulama otomatik açılmazsa aşağıdaki butona dokunun."
    } else {
        escapeHtml(reasonText ?: "Yetkilendirme tamamlanamadı. Uygulamaya dönüp tekrar deneyebilirsiniz.")
    }

    return """
        <!DOCTYPE html>
        <html lang="tr">
        <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <meta http-equiv="refresh" content="1;url=$safeUrlAttr" />
            <title>HealthTrack · Huawei Health</title>
            <style>
                body {
                    margin: 0;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: #0B0F19;
                    color: #F8FAFC;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    padding: 24px;
                    box-sizing: border-box;
                }
                .card {
                    max-width: 420px;
                    width: 100%;
                    background: rgba(18, 24, 38, 0.9);
                    border: 1px solid rgba(255, 255, 255, 0.12);
                    border-radius: 24px;
                    padding: 28px 24px;
                    text-align: center;
                    box-shadow: 0 24px 48px rgba(0, 0, 0, 0.45);
                }
                .badge {
                    display: inline-block;
                    padding: 6px 14px;
                    border-radius: 999px;
                    background: $badgeBg;
                    color: $badgeColor;
                    font-size: 12px;
                    font-weight: 700;
                    margin-bottom: 16px;
                }
                h1 {
                    margin: 0 0 10px;
                    font-size: 20px;
                    font-weight: 700;
                }
                p {
                    margin: 0 0 24px;
                    color: #94A3B8;
                    font-size: 14px;
                    line-height: 1.5;
                }
                .btn {
                    display: inline-block;
                    width: 100%;
                    box-sizing: border-box;
                    padding: 14px 18px;
                    border-radius: 14px;
                    background: linear-gradient(135deg, #0EA5E9, #10B981);
                    color: #FFFFFF;
                    font-weight: 700;
                    font-size: 15px;
                    text-decoration: none;
                }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="badge">$badgeText</div>
                <h1>$title</h1>
                <p>$subtitle</p>
                <a class="btn" href="$safeUrlAttr">HealthTrack'e Dön</a>
            </div>
            <script>
                setTimeout(function() {
                    window.location.href = "$jsUrl";
                }, 250);
            </script>
        </body>
        </html>
    """.trimIndent()
}

private data class WorkoutSportDisplay(
    val emoji: String,
    val sportKey: String,
    val phraseTr: String,
    val phraseEn: String
)

private val kortRegex = Regex("""\bkort(ta|da)?\b""")
private val potaRegex = Regex("""\bpota(ya|da)?\b""")
private val kaleRegex = Regex("""\bkale(de|si|ye)?\b""")
private val gymRegex = Regex("""\bgym\b""")
private val runWordRegex = Regex("""\b(run|runs|koşu|kosu|koşma|kosma)\b""")

private fun detectWorkoutSportDisplay(rawText: String): WorkoutSportDisplay {
    val lower = rawText.lowercase()
    return when {
        lower.contains("tenis") || lower.contains("tennis") || kortRegex.containsMatchIn(lower) ->
            WorkoutSportDisplay("🎾", "TENNIS", "tenis oynadı", "played tennis")
        lower.contains("basketbol") || lower.contains("basketball") || lower.contains("basket") || potaRegex.containsMatchIn(lower) ->
            WorkoutSportDisplay("🏀", "BASKETBALL", "basketbol oynadı", "played basketball")
        lower.contains("futbol") || lower.contains("football") || lower.contains("soccer") || lower.contains("halı saha") || lower.contains("hali saha") || kaleRegex.containsMatchIn(lower) ->
            WorkoutSportDisplay("⚽", "FOOTBALL", "futbol oynadı", "played football")
        lower.contains("voleybol") || lower.contains("volleyball") || lower.contains("plaj voleybolu") || lower.contains("smaç") || lower.contains("smac") ->
            WorkoutSportDisplay("🏐", "VOLLEYBALL", "voleybol oynadı", "played volleyball")
        lower.contains("boks") || lower.contains("boxing") || lower.contains("kickboks") || lower.contains("kickbox") || lower.contains("mma") || lower.contains("muay thai") || lower.contains("dövüş") || lower.contains("dovus") || lower.contains("karate") || lower.contains("taekwondo") || lower.contains("tekvando") || lower.contains("judo") || lower.contains("güreş") || lower.contains("gures") ->
            WorkoutSportDisplay("🥊", "BOXING", "boks yaptı", "did a boxing session")
        lower.contains("kürek") || lower.contains("kurek") || lower.contains("rowing") || lower.contains("ergometre") || lower.contains("ergometer") ->
            WorkoutSportDisplay("🚣", "ROWING", "kürek çekti", "did a rowing session")
        lower.contains("hiit") || lower.contains("crossfit") || lower.contains("tabata") || lower.contains("kardiyo") || lower.contains("cardio") || lower.contains("circuit") || lower.contains("ip atlama") || lower.contains("jump rope") || lower.contains("burpee") ->
            WorkoutSportDisplay("⚡", "HIIT", "HIIT antrenmanı yaptı", "did a HIIT workout")
        lower.contains("kalistenik") || lower.contains("calisthenics") || lower.contains("street workout") || lower.contains("barfiks") || lower.contains("pull-up") || lower.contains("pullup") || lower.contains("şınav") || lower.contains("sinav") || lower.contains("push-up") || lower.contains("pushup") || lower.contains("dips") || lower.contains("plank") || lower.contains("cimnastik") || lower.contains("gymnastics") ->
            WorkoutSportDisplay("🤸", "CALISTHENICS", "kalistenik yaptı", "did calisthenics")
        lower.contains("dans") || lower.contains("dance") || lower.contains("zumba") || lower.contains("aerobik") || lower.contains("aerobics") || lower.contains("step aerobik") || lower.contains("salsa") || lower.contains("hip hop") ->
            WorkoutSportDisplay("💃", "DANCE", "dans antrenmanı yaptı", "did a dance workout")
        lower.contains("fitness") || lower.contains("ağırlık") || lower.contains("agirlik") || lower.contains("dambıl") || lower.contains("dambil") || lower.contains("dumbell") || lower.contains("dumbbell") || lower.contains("bodybuilding") || lower.contains("hipertrofi") || lower.contains("strength") || lower.contains("weight training") || lower.contains("weights") || gymRegex.containsMatchIn(lower) || lower.contains("bench") || lower.contains("squat") || lower.contains("deadlift") ->
            WorkoutSportDisplay("🏋️", "FITNESS", "fitness yaptı", "did a fitness workout")
        lower.contains("koşu") || lower.contains("kosu") || lower.contains("koş") || lower.contains("running") || lower.contains("jogging") || lower.contains("treadmill") || lower.contains("koşu bandı") || lower.contains("kosu bandi") || runWordRegex.containsMatchIn(lower) ->
            WorkoutSportDisplay("🏃", "RUNNING", "koşu yaptı", "went for a run")
        lower.contains("bisiklet") || lower.contains("cycling") || lower.contains("eliptik") || lower.contains("elliptical") || lower.contains("pedal") || lower.contains("spin") ->
            WorkoutSportDisplay("🚴", "CYCLING", "bisiklet sürdü", "went cycling")
        lower.contains("yüzme") || lower.contains("yuzme") || lower.contains("swimming") || lower.contains("swim") || lower.contains("havuz") ->
            WorkoutSportDisplay("🏊", "SWIMMING", "yüzdü", "went swimming")
        lower.contains("yürüyüş") || lower.contains("yuruyus") || lower.contains("walking") || lower.contains("yürü") || lower.contains("yuru") || lower.contains("trekking") || lower.contains("hiking") ->
            WorkoutSportDisplay("🚶", "WALKING", "yürüyüş yaptı", "went for a walk")
        lower.contains("yoga") || lower.contains("pilates") || lower.contains("esneme") || lower.contains("stretching") ->
            WorkoutSportDisplay("🧘", "YOGA_PILATES", "yoga & pilates yaptı", "did yoga & pilates")
        else ->
            WorkoutSportDisplay("💪", "OTHER", "antrenman yaptı", "completed a workout")
    }
}
