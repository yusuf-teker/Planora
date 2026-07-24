package com.yusufteker.planora.shared.api

import kotlinx.serialization.Serializable

@Serializable
data class RegisterFcmTokenRequest(
    val token: String,
    val platform: String
)

@Serializable
data class UnregisterFcmTokenRequest(
    val token: String
)
