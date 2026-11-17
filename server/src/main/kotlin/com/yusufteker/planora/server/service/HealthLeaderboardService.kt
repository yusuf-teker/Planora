package com.yusufteker.planora.server.service

import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class HealthLeaderboardEntryDto(
    val rank: Int,
    val userId: Int,
    val name: String,
    val username: String,
    val avatarId: String = "avatar_1",
    val profileImageUrl: String? = null,
    val isPremium: Boolean = false,
    val isCurrentUser: Boolean = false,
    val isFollowedByMe: Boolean = false,
    val score: Int = 0,
    val primaryValueLabel: String = "",
    val secondaryLabel: String = "",
    val streakDays: Int = 0,
    val caloriesBurned: Int = 0,
    val workoutMinutes: Int = 0,
    val healthyEatingScore: Int = 0,
    val activeDays: Int = 0
)

@Serializable
data class HealthLeaderboardResponseDto(
    val period: String,
    val scope: String,
    val category: String,
    val startDate: String,
    val endDate: String,
    val totalParticipants: Int,
    val friendsCount: Int,
    val entries: List<HealthLeaderboardEntryDto>,
    val myEntry: HealthLeaderboardEntryDto? = null
)

@Serializable
private data class LeaderboardFoodItemMacro(
    val proteinG: Double = 0.0,
    val fiberG: Double = 0.0
)

object HealthLeaderboardService {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val istanbulZone = ZoneId.of("Europe/Istanbul")

    private const val GLOBAL_CACHE_TTL_MS = 60_000L

    private data class CachedGlobalLeaderboard(
        val period: String,
        val category: String,
        val startDate: String,
        val endDate: String,
        val sortedEntries: List<HealthLeaderboardEntryDto>,
        val computedAtMs: Long
    )

    private val globalCache = ConcurrentHashMap<String, CachedGlobalLeaderboard>()

    suspend fun getLeaderboard(
        currentUserId: Int,
        period: String,
        scope: String,
        category: String,
        dateIso: String?
    ): HealthLeaderboardResponseDto {
        val normalizedPeriod = if (period.equals("DAILY", ignoreCase = true)) "DAILY" else "WEEKLY"
        val normalizedScope = if (scope.equals("FRIENDS", ignoreCase = true)) "FRIENDS" else "GLOBAL"
        val normalizedCategory = when (category.uppercase()) {
            "CALORIES_BURNED" -> "CALORIES_BURNED"
            "HEALTHY_EATING" -> "HEALTHY_EATING"
            "WORKOUT_MINUTES" -> "WORKOUT_MINUTES"
            else -> "DISCIPLINE_XP"
        }

        val targetDate = parseDateOrToday(dateIso)

        if (normalizedScope == "GLOBAL") {
            val cacheKey = "${normalizedPeriod}|${normalizedCategory}|$targetDate"
            val cached = globalCache[cacheKey]
            val nowMs = System.currentTimeMillis()
            if (cached != null && (nowMs - cached.computedAtMs) <= GLOBAL_CACHE_TTL_MS) {
                val (followedIds, friendCount) = dbQuery {
                    val followed = FollowerEntity.find { FollowersTable.followerId eq currentUserId }
                        .map { it.followed.id.value }.toSet()
                    val followers = FollowerEntity.find { FollowersTable.followedId eq currentUserId }
                        .map { it.follower.id.value }.toSet()
                    followed to (followed + followers).size
                }
                val personalized = cached.sortedEntries.map { entry ->
                    entry.copy(
                        isCurrentUser = entry.userId == currentUserId,
                        isFollowedByMe = followedIds.contains(entry.userId)
                    )
                }
                return HealthLeaderboardResponseDto(
                    period = cached.period,
                    scope = normalizedScope,
                    category = cached.category,
                    startDate = cached.startDate,
                    endDate = cached.endDate,
                    totalParticipants = personalized.size,
                    friendsCount = friendCount,
                    entries = personalized.take(50),
                    myEntry = personalized.firstOrNull { it.isCurrentUser }
                )
            }
        }

        val (startDate, endDate) = if (normalizedPeriod == "DAILY") {
            targetDate to targetDate
        } else {
            val monday = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            monday to monday.plusDays(6)
        }
        val startIso = startDate.toString()
        val endIso = endDate.toString()
        val streakLookbackIso = targetDate.minusDays(45).toString()
        val targetIso = targetDate.toString()

        return dbQuery {
            val followedIds = FollowerEntity.find { FollowersTable.followerId eq currentUserId }
                .map { it.followed.id.value }
                .toSet()
            val followerIds = FollowerEntity.find { FollowersTable.followedId eq currentUserId }
                .map { it.follower.id.value }
                .toSet()
            val friendIds = followedIds + followerIds

            // Determine candidate users:
            // - FRIENDS always includes mutual/connected friends + currentUserId
            // - GLOBAL includes all registered users (guaranteed superset of FRIENDS)
            val participantIds: Set<Int> = if (normalizedScope == "FRIENDS") {
                friendIds + currentUserId
            } else {
                val registeredUsers = UsersTable
                    .selectAll()
                    .map { it[UsersTable.id].value }
                val nutritionGoalUsers = HealthNutritionGoalsTable
                    .selectAll()
                    .where { HealthNutritionGoalsTable.isDeleted eq false }
                    .map { it[HealthNutritionGoalsTable.userId].value }
                val foodUsers = HealthFoodLogsTable
                    .selectAll()
                    .where {
                        (HealthFoodLogsTable.isDeleted eq false) and
                            (HealthFoodLogsTable.date greaterEq startIso) and
                            (HealthFoodLogsTable.date lessEq endIso)
                    }
                    .map { it[HealthFoodLogsTable.userId].value }
                val workoutUsers = HealthWorkoutsTable
                    .selectAll()
                    .where {
                        (HealthWorkoutsTable.isDeleted eq false) and
                            (HealthWorkoutsTable.date greaterEq startIso) and
                            (HealthWorkoutsTable.date lessEq endIso)
                    }
                    .map { it[HealthWorkoutsTable.userId].value }
                val weightUsers = HealthWeightsTable
                    .selectAll()
                    .where {
                        (HealthWeightsTable.isDeleted eq false) and
                            (HealthWeightsTable.date greaterEq startIso) and
                            (HealthWeightsTable.date lessEq endIso)
                    }
                    .map { it[HealthWeightsTable.userId].value }
                (registeredUsers + nutritionGoalUsers + foodUsers + workoutUsers + weightUsers + friendIds + currentUserId).toSet()
            }

            if (participantIds.isEmpty()) {
                return@dbQuery HealthLeaderboardResponseDto(
                    period = normalizedPeriod,
                    scope = normalizedScope,
                    category = normalizedCategory,
                    startDate = startIso,
                    endDate = endIso,
                    totalParticipants = 0,
                    friendsCount = friendIds.size,
                    entries = emptyList(),
                    myEntry = null
                )
            }

            val idList = participantIds.toList()
            val usersById = UsersTable.selectAll()
                .where { UsersTable.id inList idList }
                .associateBy { it[UsersTable.id].value }

            val goalsByUserId = HealthNutritionGoalsTable.selectAll()
                .where { (HealthNutritionGoalsTable.userId inList idList) and (HealthNutritionGoalsTable.isDeleted eq false) }
                .associateBy { it[HealthNutritionGoalsTable.userId].value }

            // Fetch lookback 45 days for streak + filter range in memory for period stats
            val foodRows = HealthFoodLogsTable.selectAll()
                .where {
                    (HealthFoodLogsTable.userId inList idList) and
                        (HealthFoodLogsTable.isDeleted eq false) and
                        (HealthFoodLogsTable.date greaterEq streakLookbackIso) and
                        (HealthFoodLogsTable.date lessEq targetIso.maxDate(endIso))
                }
                .toList()

            val workoutRows = HealthWorkoutsTable.selectAll()
                .where {
                    (HealthWorkoutsTable.userId inList idList) and
                        (HealthWorkoutsTable.isDeleted eq false) and
                        (HealthWorkoutsTable.date greaterEq streakLookbackIso) and
                        (HealthWorkoutsTable.date lessEq targetIso.maxDate(endIso))
                }
                .toList()

            val weightRows = HealthWeightsTable.selectAll()
                .where {
                    (HealthWeightsTable.userId inList idList) and
                        (HealthWeightsTable.isDeleted eq false) and
                        (HealthWeightsTable.date greaterEq streakLookbackIso) and
                        (HealthWeightsTable.date lessEq targetIso.maxDate(endIso))
                }
                .toList()

            val foodsByUser = foodRows.groupBy { it[HealthFoodLogsTable.userId].value }
            val workoutsByUser = workoutRows.groupBy { it[HealthWorkoutsTable.userId].value }
            val weightsByUser = weightRows.groupBy { it[HealthWeightsTable.userId].value }

            val unsorted = idList.mapNotNull { uid ->
                val userRow = usersById[uid] ?: return@mapNotNull null
                val goalRow = goalsByUserId[uid]
                val calTarget = (goalRow?.get(HealthNutritionGoalsTable.calories) ?: 2000).coerceAtLeast(1000)
                val proteinTarget = (goalRow?.get(HealthNutritionGoalsTable.proteinG) ?: 80.0).coerceAtLeast(35.0)
                val fiberTarget = (goalRow?.get(HealthNutritionGoalsTable.fiberG) ?: 25.0).coerceAtLeast(12.0)

                val userFoodsAll = foodsByUser[uid].orEmpty()
                val userWorkoutsAll = workoutsByUser[uid].orEmpty()
                val userWeightsAll = weightsByUser[uid].orEmpty()
                val userWeightsPeriod = userWeightsAll.filter {
                    val d = it[HealthWeightsTable.date]
                    d >= startIso && d <= endIso
                }

                val userFoodsPeriod = userFoodsAll.filter {
                    val d = it[HealthFoodLogsTable.date]
                    d >= startIso && d <= endIso && it[HealthFoodLogsTable.totalCalories] > 0
                }
                val userWorkoutsPeriod = userWorkoutsAll.filter {
                    val d = it[HealthWorkoutsTable.date]
                    d >= startIso && d <= endIso &&
                        (it[HealthWorkoutsTable.caloriesBurned] > 0 || it[HealthWorkoutsTable.durationMinutes] > 0) &&
                        !isDailyStepsOrBackfill(
                            id = it[HealthWorkoutsTable.id].value,
                            rawText = it[HealthWorkoutsTable.rawText],
                            dateIso = d,
                            createdAt = it[HealthWorkoutsTable.createdAt]
                        )
                }

                val caloriesBurned = userWorkoutsPeriod.sumOf { it[HealthWorkoutsTable.caloriesBurned].coerceAtLeast(0) }
                val workoutMinutes = userWorkoutsPeriod.sumOf { it[HealthWorkoutsTable.durationMinutes].coerceAtLeast(0) }

                // Daily nutrition quality scores
                val foodScoresByDate = mutableMapOf<String, Int>()
                for (f in userFoodsPeriod) {
                    val d = f[HealthFoodLogsTable.date]
                    val kcal = f[HealthFoodLogsTable.totalCalories]
                    val macros = parseMacros(f[HealthFoodLogsTable.itemsJson])
                    val calRatio = kcal.toDouble() / calTarget.toDouble()
                    val calScore = if (calRatio in 0.85..1.15) {
                        45.0
                    } else {
                        (45.0 - abs(1.0 - calRatio) * 80.0).coerceIn(0.0, 45.0)
                    }
                    val protScore = (macros.first / proteinTarget).coerceIn(0.0, 1.0) * 35.0
                    val fiberScore = (macros.second / fiberTarget).coerceIn(0.0, 1.0) * 20.0
                    val dayScore = (calScore + protScore + fiberScore).roundToInt().coerceIn(0, 100)
                    foodScoresByDate[d] = maxOf(foodScoresByDate[d] ?: 0, dayScore)
                }

                val totalHealthyScore = foodScoresByDate.values.sum()
                val avgHealthyScore = if (foodScoresByDate.isNotEmpty()) {
                    foodScoresByDate.values.average().roundToInt()
                } else 0

                val foodDatesPeriod = foodScoresByDate.keys
                val workoutDatesPeriod = userWorkoutsPeriod.map { it[HealthWorkoutsTable.date] }.toSet()
                val weightDatesPeriod = userWeightsPeriod.filter {
                    !isWeightBackfill(
                        note = it[HealthWeightsTable.note],
                        dateIso = it[HealthWeightsTable.date],
                        createdAt = it[HealthWeightsTable.createdAt]
                    )
                }.map { it[HealthWeightsTable.date] }.toSet()
                val activeDatesPeriod = foodDatesPeriod + workoutDatesPeriod + weightDatesPeriod

                val disciplineXp = foodDatesPeriod.sumOf { d ->
                    val bonus = if ((foodScoresByDate[d] ?: 0) >= 70) 15 else 0
                    25 + bonus
                } + (workoutDatesPeriod.size * 30) + (weightDatesPeriod.size * 10)

                val nonStepWorkoutDatesAll = userWorkoutsAll.filter {
                    (it[HealthWorkoutsTable.caloriesBurned] > 0 || it[HealthWorkoutsTable.durationMinutes] > 0) &&
                        !isDailyStepsOrBackfill(
                            id = it[HealthWorkoutsTable.id].value,
                            rawText = it[HealthWorkoutsTable.rawText],
                            dateIso = it[HealthWorkoutsTable.date],
                            createdAt = it[HealthWorkoutsTable.createdAt]
                        )
                }.map { it[HealthWorkoutsTable.date] }.toSet()

                val validWeightDatesAll = userWeightsAll.filter {
                    it[HealthWeightsTable.weightKg] > 0.0 &&
                        !isWeightBackfill(
                            note = it[HealthWeightsTable.note],
                            dateIso = it[HealthWeightsTable.date],
                            createdAt = it[HealthWeightsTable.createdAt]
                        )
                }.map { it[HealthWeightsTable.date] }.toSet()

                val streakDays = computeStreakDays(
                    targetDate = targetDate,
                    activeDates = userFoodsAll.filter { it[HealthFoodLogsTable.totalCalories] > 0 }.map { it[HealthFoodLogsTable.date] }.toSet() +
                        nonStepWorkoutDatesAll +
                        validWeightDatesAll
                )

                val isDaily = normalizedPeriod == "DAILY"
                val (score, primaryLabel, secondaryLabel) = when (normalizedCategory) {
                    "CALORIES_BURNED" -> Triple(
                        caloriesBurned,
                        "$caloriesBurned kcal",
                        if (isDaily) "$workoutMinutes dk" else "$workoutMinutes dk • ${workoutDatesPeriod.size} gün"
                    )
                    "WORKOUT_MINUTES" -> Triple(
                        workoutMinutes,
                        "$workoutMinutes dk",
                        if (isDaily) "$caloriesBurned kcal" else "$caloriesBurned kcal • ${workoutDatesPeriod.size} gün"
                    )
                    "HEALTHY_EATING" -> Triple(
                        if (isDaily) avgHealthyScore else totalHealthyScore,
                        if (isDaily) "$avgHealthyScore / 100" else "$totalHealthyScore p",
                        if (foodDatesPeriod.isEmpty()) {
                            "Henüz öğün yok"
                        } else if (isDaily) {
                            "ortalama %$avgHealthyScore"
                        } else {
                            "${foodDatesPeriod.size} gün • ort %$avgHealthyScore"
                        }
                    )
                    else -> Triple(
                        disciplineXp,
                        "$disciplineXp XP",
                        if (isDaily) {
                            if (workoutMinutes > 0) "$caloriesBurned kcal • $workoutMinutes dk" else "$caloriesBurned kcal"
                        } else {
                            "${activeDatesPeriod.size} aktif gün • $caloriesBurned kcal"
                        }
                    )
                }

                val isUserPremium = userRow[UsersTable.isPremium] &&
                    (userRow[UsersTable.premiumUntil]?.isAfter(Instant.now()) ?: true)

                HealthLeaderboardEntryDto(
                    rank = 0,
                    userId = uid,
                    name = userRow[UsersTable.name],
                    username = userRow[UsersTable.username],
                    avatarId = userRow[UsersTable.avatarId],
                    profileImageUrl = userRow[UsersTable.profileImageUrl],
                    isPremium = isUserPremium,
                    isCurrentUser = uid == currentUserId,
                    isFollowedByMe = followedIds.contains(uid),
                    score = score,
                    primaryValueLabel = primaryLabel,
                    secondaryLabel = secondaryLabel,
                    streakDays = streakDays,
                    caloriesBurned = caloriesBurned,
                    workoutMinutes = workoutMinutes,
                    healthyEatingScore = if (normalizedPeriod == "DAILY") avgHealthyScore else totalHealthyScore,
                    activeDays = activeDatesPeriod.size
                )
            }

            val sorted = unsorted.sortedWith(
                compareByDescending<HealthLeaderboardEntryDto> { it.score }
                    .thenByDescending { it.streakDays }
                    .thenByDescending { it.activeDays }
                    .thenByDescending { it.isCurrentUser }
                    .thenBy { it.name.lowercase() }
            ).mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }

            val myEntry = sorted.firstOrNull { it.isCurrentUser }
            val topEntries = sorted.take(50)

            if (normalizedScope == "GLOBAL") {
                val cacheKey = "${normalizedPeriod}|${normalizedCategory}|$targetDate"
                globalCache[cacheKey] = CachedGlobalLeaderboard(
                    period = normalizedPeriod,
                    category = normalizedCategory,
                    startDate = startIso,
                    endDate = endIso,
                    sortedEntries = sorted.map { it.copy(isCurrentUser = false, isFollowedByMe = false) },
                    computedAtMs = System.currentTimeMillis()
                )
            }

            HealthLeaderboardResponseDto(
                period = normalizedPeriod,
                scope = normalizedScope,
                category = normalizedCategory,
                startDate = startIso,
                endDate = endIso,
                totalParticipants = sorted.size,
                friendsCount = friendIds.size,
                entries = topEntries,
                myEntry = myEntry
            )
        }
    }

    private fun parseMacros(itemsJson: String): Pair<Double, Double> {
        if (itemsJson.isBlank() || itemsJson == "[]") return 0.0 to 0.0
        return try {
            val items = json.decodeFromString<List<LeaderboardFoodItemMacro>>(itemsJson)
            var protein = 0.0
            var fiber = 0.0
            for (item in items) {
                protein += item.proteinG.coerceAtLeast(0.0)
                fiber += item.fiberG.coerceAtLeast(0.0)
            }
            protein to fiber
        } catch (_: Exception) {
            0.0 to 0.0
        }
    }

    private fun isDailyStepsOrBackfill(
        id: String,
        rawText: String,
        dateIso: String,
        createdAt: java.time.Instant
    ): Boolean {
        if (id.endsWith("_steps") ||
            id.contains("daily_steps") ||
            rawText.contains("Adım • Günlük Hareket", ignoreCase = true) ||
            rawText.contains("Steps • Daily Activity", ignoreCase = true)
        ) {
            return true
        }
        if (id.startsWith("wdev_")) {
            val syncedDate = java.time.LocalDate.ofInstant(createdAt, istanbulZone)
            if (dateIso < syncedDate.minusDays(1).toString()) {
                return true
            }
        }
        return false
    }

    private fun isWeightBackfill(
        note: String?,
        dateIso: String,
        createdAt: java.time.Instant
    ): Boolean {
        if (note != null && note.startsWith("⚖️")) {
            val syncedDate = java.time.LocalDate.ofInstant(createdAt, istanbulZone)
            if (dateIso < syncedDate.minusDays(1).toString()) {
                return true
            }
        }
        return false
    }

    private fun computeStreakDays(targetDate: LocalDate, activeDates: Set<String>): Int {
        if (activeDates.isEmpty()) return 0
        var cursor = if (activeDates.contains(targetDate.toString())) {
            targetDate
        } else if (activeDates.contains(targetDate.minusDays(1).toString())) {
            targetDate.minusDays(1)
        } else {
            return 0
        }
        var streak = 0
        while (activeDates.contains(cursor.toString())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun parseDateOrToday(dateIso: String?): LocalDate {
        if (!dateIso.isNullOrBlank()) {
            try {
                return LocalDate.parse(dateIso.trim())
            } catch (_: Exception) {
            }
        }
        return LocalDate.now(istanbulZone)
    }

    private fun String.maxDate(other: String): String = if (this >= other) this else other
}

