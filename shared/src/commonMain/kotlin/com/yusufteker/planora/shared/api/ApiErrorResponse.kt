package com.yusufteker.planora.shared.api

import kotlinx.serialization.Serializable

/**
 * Standard error response structure sent by the Planora server.
 *
 * Inspired by RFC 7807 / RFC 9457 (Problem Details for HTTP APIs), this structure provides:
 * - [code]: Machine-readable error code for client logic and localization keys
 * - [message]: High-level summary of the error for developers / debugging
 * - [fieldErrors]: Field-by-field validation failure reasons for direct UI input binding
 */
@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String? = null,
    val fieldErrors: Map<String, String>? = null
)

/**
 * Exception thrown when incoming request data fails Fail-Fast validation.
 *
 * @param fieldErrors Map of parameter/field names to descriptive error explanations
 * @param message High-level summary of the validation failure
 */
class ValidationException(
    val fieldErrors: Map<String, String>,
    message: String = "Validation failed for one or more fields"
) : IllegalArgumentException(message)

/**
 * Standardized error code constants used across Server and Client.
 */
object ApiErrorCode {
    const val EMAIL_IN_USE = "ERR_EMAIL_IN_USE"
    const val USERNAME_IN_USE = "ERR_USERNAME_IN_USE"
    const val INVALID_CREDENTIALS = "ERR_INVALID_CREDENTIALS"
    const val INVALID_REFRESH_TOKEN = "ERR_INVALID_REFRESH_TOKEN"
    const val INVALID_RESET_CODE = "ERR_INVALID_RESET_CODE"
    const val EXPIRED_RESET_CODE = "ERR_EXPIRED_RESET_CODE"
    const val INVALID_VERIFICATION_CODE = "ERR_INVALID_VERIFICATION_CODE"
    const val EXPIRED_VERIFICATION_CODE = "ERR_EXPIRED_VERIFICATION_CODE"
    const val ROOM_NOT_FOUND = "ERR_ROOM_NOT_FOUND"
    const val ROOM_NAME_EMPTY = "ERR_ROOM_NAME_EMPTY"
    const val ROOM_PERMISSION_DENIED = "ERR_ROOM_PERMISSION_DENIED"
    const val ROOM_ALREADY_MEMBER = "ERR_ROOM_ALREADY_MEMBER"
    const val ROOM_INVITATION_NOT_FOUND = "ERR_ROOM_INVITATION_NOT_FOUND"
    const val USER_NOT_FOUND = "ERR_USER_NOT_FOUND"
    const val UNAUTHORIZED = "ERR_UNAUTHORIZED"
    const val FORBIDDEN = "ERR_FORBIDDEN"
    const val BAD_REQUEST = "ERR_BAD_REQUEST"
    const val VALIDATION_ERROR = "ERR_VALIDATION_ERROR"
    const val TOO_MANY_REQUESTS = "ERR_TOO_MANY_REQUESTS"
    const val INTERNAL_SERVER_ERROR = "ERR_INTERNAL_SERVER_ERROR"
}
