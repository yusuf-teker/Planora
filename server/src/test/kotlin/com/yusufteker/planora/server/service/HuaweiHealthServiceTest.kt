package com.yusufteker.planora.server.service

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HuaweiHealthServiceTest {

    private val istanbulZone = ZoneId.of("Europe/Istanbul")

    @Test
    fun `HMAC state sign and verify succeeds for valid state and rejects tampered or expired`() {
        val secret = "test-jwt-secret-key-for-hmac"
        val nowMs = 1_760_000_000_000L

        val state = HuaweiHealthService.signState(userId = 42, issuedAtMs = nowMs, secret = secret)
        assertEquals(42, HuaweiHealthService.verifyState(state, nowMs = nowMs + 60_000L, secret = secret))

        // Tampered payload or signature must fail
        val parts = state.split('.')
        val tampered = parts[0] + ".tamperedSig123"
        assertNull(HuaweiHealthService.verifyState(tampered, nowMs = nowMs + 1000L, secret = secret))

        // Different secret must fail
        assertNull(HuaweiHealthService.verifyState(state, nowMs = nowMs + 1000L, secret = "wrong-secret"))

        // Expired (> 15 min) must fail
        assertNull(HuaweiHealthService.verifyState(state, nowMs = nowMs + 20L * 60_000L, secret = secret))

        // Malformed format
        assertNull(HuaweiHealthService.verifyState("invalid-state-without-dot", nowMs = nowMs, secret = secret))
    }

    @Test
    fun `normalizeTimestampMillis handles seconds milliseconds and nanoseconds`() {
        assertEquals(1_700_000_000_000L, HuaweiHealthService.normalizeTimestampMillis(1_700_000_000_000L))
        assertEquals(1_700_000_000_123L, HuaweiHealthService.normalizeTimestampMillis(1_700_000_000_123_000_000L))
        assertEquals(1_700_000_000_000L, HuaweiHealthService.normalizeTimestampMillis(1_700_000_000L))
    }

    @Test
    fun `parseActivityRecordsJson parses ms and ns timestamps, telemetry and maps sports`() {
        val sampleJson = """
            {
              "activityRecord": [
                {
                  "id": "act_run_1",
                  "name": "Morning Outdoor Run",
                  "desc": "Sabah Koşusu",
                  "activityType": 56,
                  "startTime": 1763877600000,
                  "endTime": 1763880000000,
                  "activitySummary": {
                    "dataSummary": [
                      {
                        "dataTypeName": "com.huawei.continuous.calories.burnt.total",
                        "value": [{ "fieldName": "calories_total", "floatValue": 418.6 }]
                      },
                      {
                        "dataTypeName": "com.huawei.continuous.distance.total",
                        "value": [{ "fieldName": "distance", "floatValue": 6250.4 }]
                      },
                      {
                        "dataTypeName": "com.huawei.continuous.steps.total",
                        "value": [{ "fieldName": "steps_total", "integerValue": 7420 }]
                      },
                      {
                        "dataTypeName": "com.huawei.continuous.exercise_heart_rate.statistics",
                        "value": [
                          { "fieldName": "avg", "floatValue": 152.3 },
                          { "fieldName": "max", "floatValue": 176.0 }
                        ]
                      }
                    ]
                  }
                },
                {
                  "id": "act_tennis_2",
                  "name": "Evening Tennis Match",
                  "activityType": 999,
                  "startTime": 1763910000000000000,
                  "endTime": 1763913600000000000,
                  "activitySummary": {
                    "dataSummary": [
                      {
                        "dataTypeName": "com.huawei.continuous.calories.burnt.total",
                        "value": [{ "fieldName": "calories", "floatValue": 520.0 }]
                      }
                    ]
                  }
                },
                {
                  "id": "act_yoga_3",
                  "name": "Hatha Yoga",
                  "startTime": 1763917200000,
                  "endTime": 1763919000000
                }
              ],
              "hasMoreData": false
            }
        """.trimIndent()

        val workouts = HuaweiHealthService.parseActivityRecordsJson(sampleJson, istanbulZone)
        assertEquals(3, workouts.size)

        val run = workouts[0]
        assertEquals("act_run_1", run.externalId)
        assertEquals("RUNNING", run.category)
        assertEquals(40, run.durationMinutes)
        assertEquals(419, run.caloriesBurned)
        assertEquals(6250.4, run.distanceMeters)
        assertEquals(7420, run.steps)
        assertEquals(152, run.avgHeartRateBpm)
        assertEquals("Huawei Health", run.sourceName)
        assertEquals("com.huawei.health", run.sourcePackage)

        val tennis = workouts[1]
        assertEquals("TENNIS", tennis.category)
        assertEquals(60, tennis.durationMinutes)
        assertEquals(520, tennis.caloriesBurned)
        assertEquals(1_763_910_000_000L, tennis.startTimeMillis)

        val yoga = workouts[2]
        assertEquals("YOGA", yoga.category)
        assertEquals(30, yoga.durationMinutes)
    }

    @Test
    fun `parseDailySummaryPolymerizeJson and workout subtraction avoids double counting`() {
        // 2026-11-22 00:00:00 UTC+3 = 1795294800000 ms (let's use any deterministic epoch ms)
        val dayStartMs = 1_763_845_200_000L
        val dayEndMs = dayStartMs + 86_399_000L

        val polymerizeJson = """
            {
              "group": [
                {
                  "startTime": "$dayStartMs",
                  "endTime": "$dayEndMs",
                  "sampleSet": [
                    {
                      "dataCollectorId": "derived:com.huawei.continuous.steps.delta:com.huawei.hms.hihealth.service:polymerized",
                      "samplePoints": [
                        {
                          "startTime": ${dayStartMs * 1_000_000L},
                          "endTime": ${dayEndMs * 1_000_000L},
                          "dataTypeName": "com.huawei.continuous.steps.delta",
                          "value": [{ "fieldName": "steps", "integerValue": 10000 }]
                        }
                      ]
                    },
                    {
                      "dataCollectorId": "derived:com.huawei.continuous.calories.burnt:com.huawei.hms.hihealth.service:polymerized",
                      "samplePoints": [
                        {
                          "dataTypeName": "com.huawei.continuous.calories.burnt",
                          "value": [{ "fieldName": "calories", "floatValue": 650.0 }]
                        }
                      ]
                    },
                    {
                      "dataCollectorId": "derived:com.huawei.continuous.distance.delta:com.huawei.hms.hihealth.service:polymerized",
                      "samplePoints": [
                        {
                          "dataTypeName": "com.huawei.continuous.distance.delta",
                          "value": [{ "fieldName": "distance", "floatValue": 8000.0 }]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val buckets = HuaweiHealthService.parseDailySummaryPolymerizeJson(polymerizeJson, istanbulZone)
        assertEquals(1, buckets.size)
        val bucket = buckets.first()
        assertEquals(10000, bucket.steps)
        assertEquals(650, bucket.caloriesKcal)
        assertEquals(8000.0, bucket.distanceMeters)

        val structuredWorkout = HuaweiDeviceWorkoutDto(
            externalId = "run_1",
            date = bucket.date,
            category = "RUNNING",
            durationMinutes = 35,
            caloriesBurned = 400,
            distanceMeters = 5000.0,
            steps = 6000,
            startTimeMillis = dayStartMs + 3_600_000L,
            endTimeMillis = dayStartMs + 5_700_000L
        )

        val dailySteps = HuaweiHealthService.subtractWorkoutsFromDailySummaries(
            dailyBuckets = buckets,
            structuredWorkouts = listOf(structuredWorkout),
            nowMs = dayEndMs
        )
        assertEquals(1, dailySteps.size)
        val remaining = dailySteps.first()
        assertEquals("DAILY_STEPS", remaining.category)
        assertEquals(4000, remaining.steps)
        assertEquals(250, remaining.caloriesBurned)
        assertEquals(3000.0, remaining.distanceMeters)
        assertEquals("hw_steps_${bucket.date}", remaining.externalId)

        // If structured workout already accounts for all steps (<20 remaining), no DAILY_STEPS is emitted
        val fullWorkout = structuredWorkout.copy(steps = 10_000)
        val emptyDaily = HuaweiHealthService.subtractWorkoutsFromDailySummaries(
            dailyBuckets = buckets,
            structuredWorkouts = listOf(fullWorkout),
            nowMs = dayEndMs
        )
        assertTrue(emptyDaily.isEmpty())
    }

    @Test
    fun `parseBodyWeightsPolymerizeJson extracts weight, body fat, muscle, bone salt and impedance`() {
        val ts1 = 1_763_874_000_000L
        val ts2 = 1_763_960_400_000L
        val weightJson = """
            {
              "group": [
                {
                  "startTime": $ts1,
                  "endTime": $ts2,
                  "sampleSet": [
                    {
                      "dataTypeName": "com.huawei.instantaneous.body_weight",
                      "samplePoints": [
                        {
                          "startTime": ${ts1 * 1_000_000L},
                          "endTime": ${ts1 * 1_000_000L},
                          "dataTypeName": "com.huawei.instantaneous.body_weight",
                          "value": [
                            { "fieldName": "body_weight", "floatValue": 74.6 },
                            { "fieldName": "body_fat_rate", "floatValue": 16.4 },
                            { "fieldName": "muscle_mass", "floatValue": 59.1 },
                            { "fieldName": "bone_salt", "floatValue": 3.1 },
                            { "fieldName": "impedance", "floatValue": 482.2 }
                          ]
                        },
                        {
                          "startTime": $ts2,
                          "endTime": $ts2,
                          "value": [
                            { "fieldName": "body_weight", "floatValue": 74.2 },
                            { "fieldName": "skeletal_musclel_mass", "floatValue": 34.5 }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val weights = HuaweiHealthService.parseBodyWeightsPolymerizeJson(weightJson, istanbulZone)
        assertEquals(2, weights.size)

        val first = weights[0]
        assertEquals("hw_weight_$ts1", first.externalId)
        assertEquals(74.6, first.weightKg)
        assertEquals(16.4, first.bodyFatPercent)
        assertEquals(59.1, first.muscleMassKg)
        assertEquals(3.1, first.boneMassKg)
        assertEquals(482, first.impedanceOhm)
        assertEquals("Huawei Health", first.sourceName)

        val second = weights[1]
        assertEquals(74.2, second.weightKg)
        assertEquals(34.5, second.muscleMassKg)
        assertNotNull(second.date)
    }
}
