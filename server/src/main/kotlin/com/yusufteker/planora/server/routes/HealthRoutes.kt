package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.*
import com.yusufteker.planora.server.service.HealthAiService
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
    val date: String
)

@Serializable
data class WorkoutAiRequest(
    val text: String,
    val userWeightKg: Double? = null,
    val date: String
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
    }

    // 2. Authenticated HealthTrack APIs
    authenticate("auth-jwt") {
        route("/health") {

            // --- AI Endpoints ---
            post("/ai/food") {
                val req = call.receive<FoodAiRequest>()
                try {
                    val result = HealthAiService.analyzeFood(req.text, req.date)
                    call.respond(HttpStatusCode.OK, result)
                } catch (e: Exception) {
                    println("[HealthRoutes] Error in /health/ai/food: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "AI food analysis failed")))
                }
            }

            post("/ai/workout") {
                val req = call.receive<WorkoutAiRequest>()
                try {
                    val result = HealthAiService.analyzeWorkout(req.text, req.userWeightKg, req.date)
                    call.respond(HttpStatusCode.OK, result)
                } catch (e: Exception) {
                    println("[HealthRoutes] Error in /health/ai/workout: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "AI workout analysis failed")))
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

                dbQuery {
                    // 1. Process Foods
                    req.foods.forEach { food ->
                        val existing = HealthFoodLogsTable.selectAll()
                            .where { (HealthFoodLogsTable.id eq food.id) and (HealthFoodLogsTable.userId eq userId) }
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
                        } else if (food.updatedAt > existing[HealthFoodLogsTable.updatedAt]) {
                            HealthFoodLogsTable.update({ HealthFoodLogsTable.id eq food.id }) {
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
                            HealthNutritionGoalsTable.insert {
                                it[id] = goal.id
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
                                it[updatedAt] = goal.updatedAt
                                it[isDeleted] = goal.isDeleted
                            }
                            syncedGoalId = goal.id
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
                            .where { (HealthWorkoutsTable.id eq w.id) and (HealthWorkoutsTable.userId eq userId) }
                            .firstOrNull()

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
                            syncedWorkoutIds.add(w.id)
                        } else if (w.updatedAt > existing[HealthWorkoutsTable.updatedAt]) {
                            HealthWorkoutsTable.update({ HealthWorkoutsTable.id eq w.id }) {
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
                            syncedWorkoutIds.add(w.id)
                        } else {
                            syncedWorkoutIds.add(w.id)
                        }
                    }

                    // 4. Process Weights
                    req.weights.forEach { wt ->
                        val existing = HealthWeightsTable.selectAll()
                            .where { (HealthWeightsTable.id eq wt.id) and (HealthWeightsTable.userId eq userId) }
                            .firstOrNull()

                        if (existing == null) {
                            HealthWeightsTable.insert {
                                it[id] = wt.id
                                it[HealthWeightsTable.userId] = userId
                                it[date] = wt.date
                                it[weightKg] = wt.weightKg
                                it[targetWeightKg] = wt.targetWeightKg
                                it[note] = wt.note
                                it[createdAt] = Instant.now()
                                it[updatedAt] = wt.updatedAt
                                it[isDeleted] = wt.isDeleted
                            }
                            syncedWeightIds.add(wt.id)
                        } else if (wt.updatedAt > existing[HealthWeightsTable.updatedAt]) {
                            HealthWeightsTable.update({ HealthWeightsTable.id eq wt.id }) {
                                it[date] = wt.date
                                it[weightKg] = wt.weightKg
                                it[targetWeightKg] = wt.targetWeightKg
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
                            .where { (HealthAppointmentsTable.id eq appt.id) and (HealthAppointmentsTable.userId eq userId) }
                            .firstOrNull()

                        var planoraTaskId = appt.planoraTaskId

                        if (appt.isDeleted) {
                            // If deleted, remove mirrored task if exists
                            existing?.get(HealthAppointmentsTable.planoraTaskId)?.value?.let { pTaskId ->
                                TasksTable.deleteWhere { TasksTable.id eq pTaskId }
                            }
                            if (existing != null) {
                                HealthAppointmentsTable.update({ HealthAppointmentsTable.id eq appt.id }) {
                                    it[isDeleted] = true
                                    it[updatedAt] = appt.updatedAt
                                    it[HealthAppointmentsTable.planoraTaskId] = null
                                }
                            }
                            syncedAppointmentIds.add(appt.id)
                            return@forEach
                        }

                        // Mirror or update Planora task (EVENT)
                        val taskTitle = "Randevu: ${appt.doctorName} - ${appt.hospitalName}"
                        val taskDesc = appt.notes ?: "Sağlık Randevusu"
                        val eventStart = appt.dateTime
                        val eventEnd = appt.dateTime + (30 * 60 * 1000L) // 30 min duration default

                        if (planoraTaskId != null && TasksTable.selectAll().where { TasksTable.id eq planoraTaskId }.count() > 0) {
                            // Update existing Planora task
                            TasksTable.update({ TasksTable.id eq planoraTaskId }) {
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
                            HealthAppointmentsTable.update({ HealthAppointmentsTable.id eq appt.id }) {
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
        }
    }
}
