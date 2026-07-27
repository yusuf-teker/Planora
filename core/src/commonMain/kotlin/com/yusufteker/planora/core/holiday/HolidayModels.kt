package com.yusufteker.planora.core.holiday

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HolidayDto(
    val name: String,
    val date: String, // Format: YYYY-MM-DD
    val day: String? = null,
    val type: String? = null,
    @SerialName("country") val country: String? = null
)

@Serializable
data class HolidayEntity(
    val date: String, // YYYY-MM-DD
    val name: String,
    val countryCode: String,
    val year: Int
)
