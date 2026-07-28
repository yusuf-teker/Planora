package com.yusufteker.planora.core.util

import com.yusufteker.planora.core.network.PlanoraApiException
import com.yusufteker.planora.core.ui.text.UiText
import com.yusufteker.planora.shared.api.ApiErrorCode
import planora.core.generated.resources.Res
import planora.core.generated.resources.error_bad_request
import planora.core.generated.resources.error_email_in_use
import planora.core.generated.resources.error_expired_reset_code
import planora.core.generated.resources.error_forbidden
import planora.core.generated.resources.error_generic
import planora.core.generated.resources.error_invalid_credentials
import planora.core.generated.resources.error_invalid_refresh_token
import planora.core.generated.resources.error_invalid_reset_code
import planora.core.generated.resources.error_network
import planora.core.generated.resources.error_room_already_member
import planora.core.generated.resources.error_room_invitation_not_found
import planora.core.generated.resources.error_room_name_empty
import planora.core.generated.resources.error_room_not_found
import planora.core.generated.resources.error_room_permission_denied
import planora.core.generated.resources.error_unauthorized
import planora.core.generated.resources.error_user_not_found
import planora.core.generated.resources.error_username_in_use

/**
 * Extension function to map any [Throwable] (including [PlanoraApiException])
 * into a localized [UiText] suitable for presentation in UI components.
 */
fun Throwable.toUiText(): UiText {
    return when (this) {
        is PlanoraApiException -> {
            when (errorResponse.code) {
                ApiErrorCode.EMAIL_IN_USE -> UiText.StringResourceId(Res.string.error_email_in_use)
                ApiErrorCode.USERNAME_IN_USE -> UiText.StringResourceId(Res.string.error_username_in_use)
                ApiErrorCode.INVALID_CREDENTIALS -> UiText.StringResourceId(Res.string.error_invalid_credentials)
                ApiErrorCode.INVALID_REFRESH_TOKEN -> UiText.StringResourceId(Res.string.error_invalid_refresh_token)
                ApiErrorCode.INVALID_RESET_CODE -> UiText.StringResourceId(Res.string.error_invalid_reset_code)
                ApiErrorCode.EXPIRED_RESET_CODE -> UiText.StringResourceId(Res.string.error_expired_reset_code)
                ApiErrorCode.ROOM_NOT_FOUND -> UiText.StringResourceId(Res.string.error_room_not_found)
                ApiErrorCode.ROOM_NAME_EMPTY -> UiText.StringResourceId(Res.string.error_room_name_empty)
                ApiErrorCode.ROOM_PERMISSION_DENIED -> UiText.StringResourceId(Res.string.error_room_permission_denied)
                ApiErrorCode.ROOM_ALREADY_MEMBER -> UiText.StringResourceId(Res.string.error_room_already_member)
                ApiErrorCode.ROOM_INVITATION_NOT_FOUND -> UiText.StringResourceId(Res.string.error_room_invitation_not_found)
                ApiErrorCode.USER_NOT_FOUND -> UiText.StringResourceId(Res.string.error_user_not_found)
                ApiErrorCode.UNAUTHORIZED -> UiText.StringResourceId(Res.string.error_unauthorized)
                ApiErrorCode.FORBIDDEN -> UiText.StringResourceId(Res.string.error_forbidden)
                ApiErrorCode.BAD_REQUEST -> UiText.StringResourceId(Res.string.error_bad_request)
                else -> {
                    // Fallback to server message if provided, otherwise generic error
                    if (!errorResponse.message.isNull_or_empty()) {
                        UiText.DynamicString(errorResponse.message!!)
                    } else {
                        UiText.StringResourceId(Res.string.error_generic)
                    }
                }
            }
        }
        else -> {
            val message = this.message ?: ""
            if (message.contains("ConnectException", ignoreCase = true) ||
                message.contains("UnknownHostException", ignoreCase = true) ||
                message.contains("SocketTimeoutException", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true)
            ) {
                UiText.StringResourceId(Res.string.error_network)
            } else if (message.isNotBlank()) {
                UiText.DynamicString(message)
            } else {
                UiText.StringResourceId(Res.string.error_generic)
            }
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
