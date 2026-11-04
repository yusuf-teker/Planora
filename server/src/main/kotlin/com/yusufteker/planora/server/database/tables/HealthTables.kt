package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * HealthTrack Food Logs Table
 */
object HealthFoodLogsTable : IdTable<String>("health_food_logs") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val userId = reference("user_id", UsersTable).index()
    val date = varchar("date", 10).index() // YYYY-MM-DD
    val rawText = text("raw_text")
    val summary = text("summary").nullable()
    val totalCalories = integer("total_calories").default(0)
    val itemsJson = text("items_json").default("[]")
    val createdAt = timestamp("created_at")
    val updatedAt = long("updated_at").index()
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}

/**
 * HealthTrack Nutrition Goals Table (1 per user)
 */
object HealthNutritionGoalsTable : IdTable<String>("health_nutrition_goals") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val calories = integer("calories").default(2000)
    val proteinG = double("protein_g").default(80.0)
    val carbsG = double("carbs_g").default(250.0)
    val fatG = double("fat_g").default(65.0)
    val fiberG = double("fiber_g").default(30.0)
    val vitaminCMg = double("vitamin_c_mg").default(90.0)
    val vitaminDUg = double("vitamin_d_ug").default(15.0)
    val vitaminB12Ug = double("vitamin_b12_ug").default(2.4)
    val ironMg = double("iron_mg").default(14.0)
    val calciumMg = double("calcium_mg").default(1000.0)
    val magnesiumMg = double("magnesium_mg").default(350.0)
    val updatedAt = long("updated_at")
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}

/**
 * HealthTrack Workouts Table
 */
object HealthWorkoutsTable : IdTable<String>("health_workouts") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val userId = reference("user_id", UsersTable).index()
    val date = varchar("date", 10).index() // YYYY-MM-DD
    val rawText = text("raw_text")
    val caloriesBurned = integer("calories_burned").default(0)
    val durationMinutes = integer("duration_minutes").default(0)
    val targetCalories = integer("target_calories").default(300)
    val targetDurationMinutes = integer("target_duration_minutes").default(45)
    val aiAnalysis = text("ai_analysis").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = long("updated_at").index()
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}

/**
 * HealthTrack Weights Table
 */
object HealthWeightsTable : IdTable<String>("health_weights") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val userId = reference("user_id", UsersTable).index()
    val date = varchar("date", 10).index() // YYYY-MM-DD
    val weightKg = double("weight_kg")
    val targetWeightKg = double("target_weight_kg").nullable()
    val note = text("note").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = long("updated_at").index()
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}

/**
 * HealthTrack Appointments Table
 */
object HealthAppointmentsTable : IdTable<String>("health_appointments") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val userId = reference("user_id", UsersTable).index()
    val dateTime = long("date_time").index() // Epoch millis
    val doctorName = varchar("doctor_name", 255)
    val hospitalName = varchar("hospital_name", 255)
    val department = varchar("department", 255).nullable()
    val notes = text("notes").nullable()
    val reminderMinutes = integer("reminder_minutes").default(60)
    val planoraTaskId = optReference("planora_task_id", TasksTable)
    val createdAt = timestamp("created_at")
    val updatedAt = long("updated_at").index()
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}

/**
 * HealthTrack Appointment Attachments Table (Metadata)
 */
object HealthAppointmentAttachmentsTable : IdTable<String>("health_appointment_attachments") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val appointmentId = reference("appointment_id", HealthAppointmentsTable).index()
    val fileName = varchar("file_name", 255)
    val fileType = varchar("file_type", 50)
    val localFilePath = varchar("local_file_path", 500)
    val fileSizeBytes = long("file_size_bytes").default(0L)
    val createdAt = timestamp("created_at")
    val updatedAt = long("updated_at")
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}
