package com.yusufteker.planora.shared.validation

import com.yusufteker.planora.shared.api.AuthRequest
import com.yusufteker.planora.shared.api.AutoScheduleRequest
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.ForgotPasswordRequest
import com.yusufteker.planora.shared.api.RegisterRequest
import com.yusufteker.planora.shared.api.RenamePlanRoomRequest
import com.yusufteker.planora.shared.api.ResetPasswordRequest
import com.yusufteker.planora.shared.api.SendRegisterCodeRequest
import com.yusufteker.planora.shared.api.ValidationException
import com.yusufteker.planora.shared.api.VerifyResetCodeRequest

/**
 * Standard regular expressions and boundary constants used for input validation.
 */
object ValidationRules {
    val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,30}\$")
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_NAME_LENGTH = 50
    const val MAX_TITLE_LENGTH = 250
    const val MAX_ROOM_NAME_LENGTH = 100
}

/**
 * Validates [RegisterRequest] following the Fail-Fast principle.
 *
 * Checks:
 * - [RegisterRequest.name]: Non-blank, maximum length of [ValidationRules.MAX_NAME_LENGTH]
 * - [RegisterRequest.username]: Valid format and length (3-30 alphanumeric + underscore)
 * - [RegisterRequest.email]: Valid email syntax via regex
 * - [RegisterRequest.password]: Minimum length of [ValidationRules.MIN_PASSWORD_LENGTH]
 * - [RegisterRequest.code]: Non-blank verification token
 *
 * @throws ValidationException when one or more validation constraints fail.
 */
fun RegisterRequest.validate() {
    val errors = mutableMapOf<String, String>()

    val trimmedName = name.trim()
    if (trimmedName.isBlank()) {
        errors["name"] = "Name cannot be blank"
    } else if (trimmedName.length > ValidationRules.MAX_NAME_LENGTH) {
        errors["name"] = "Name cannot exceed ${ValidationRules.MAX_NAME_LENGTH} characters"
    }

    val trimmedUsername = username.trim()
    if (trimmedUsername.isBlank()) {
        errors["username"] = "Username cannot be blank"
    } else if (!ValidationRules.USERNAME_REGEX.matches(trimmedUsername)) {
        errors["username"] = "Username must be 3-30 alphanumeric characters or underscores"
    }

    val trimmedEmail = email.trim()
    if (trimmedEmail.isBlank()) {
        errors["email"] = "Email cannot be blank"
    } else if (!ValidationRules.EMAIL_REGEX.matches(trimmedEmail)) {
        errors["email"] = "Invalid email format"
    }

    if (password.length < ValidationRules.MIN_PASSWORD_LENGTH) {
        errors["password"] = "Password must be at least ${ValidationRules.MIN_PASSWORD_LENGTH} characters"
    }

    if (code.trim().isBlank()) {
        errors["code"] = "Verification code cannot be blank"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [AuthRequest] (Login) following the Fail-Fast principle.
 *
 * Checks:
 * - [AuthRequest.identifier]: Non-blank username or email
 * - [AuthRequest.password]: Non-blank password
 *
 * @throws ValidationException when credentials format is invalid before querying database.
 */
fun AuthRequest.validate() {
    val errors = mutableMapOf<String, String>()

    if (identifier.trim().isBlank()) {
        errors["identifier"] = "Email or username is required"
    }
    if (password.isBlank()) {
        errors["password"] = "Password cannot be blank"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [SendRegisterCodeRequest] before checking uniqueness in database or invoking SMTP.
 *
 * Checks:
 * - [SendRegisterCodeRequest.email]: Valid syntax matching standard email regex
 * - [SendRegisterCodeRequest.username]: 3-30 characters matching username constraints
 *
 * @throws ValidationException if email or username does not conform to specifications.
 */
fun SendRegisterCodeRequest.validate() {
    val errors = mutableMapOf<String, String>()

    val trimmedEmail = email.trim()
    if (trimmedEmail.isBlank()) {
        errors["email"] = "Email cannot be blank"
    } else if (!ValidationRules.EMAIL_REGEX.matches(trimmedEmail)) {
        errors["email"] = "Invalid email format"
    }

    val trimmedUsername = username.trim()
    if (trimmedUsername.isBlank()) {
        errors["username"] = "Username cannot be blank"
    } else if (!ValidationRules.USERNAME_REGEX.matches(trimmedUsername)) {
        errors["username"] = "Username must be 3-30 alphanumeric characters or underscores"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [ForgotPasswordRequest] to guarantee an email is present and well-formed.
 *
 * @throws ValidationException if email format is invalid.
 */
fun ForgotPasswordRequest.validate() {
    val errors = mutableMapOf<String, String>()
    val trimmedEmail = email.trim()
    if (trimmedEmail.isBlank() || !ValidationRules.EMAIL_REGEX.matches(trimmedEmail)) {
        errors["email"] = "A valid email address is required"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [VerifyResetCodeRequest] before checking database tokens.
 *
 * @throws ValidationException if email or code is blank or improperly formed.
 */
fun VerifyResetCodeRequest.validate() {
    val errors = mutableMapOf<String, String>()
    val trimmedEmail = email.trim()
    if (trimmedEmail.isBlank() || !ValidationRules.EMAIL_REGEX.matches(trimmedEmail)) {
        errors["email"] = "A valid email address is required"
    }
    if (code.trim().isBlank()) {
        errors["code"] = "Verification code cannot be blank"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [ResetPasswordRequest] before updating database passwords.
 *
 * @throws ValidationException if email, verification code, or new password length fails constraints.
 */
fun ResetPasswordRequest.validate() {
    val errors = mutableMapOf<String, String>()
    val trimmedEmail = email.trim()
    if (trimmedEmail.isBlank() || !ValidationRules.EMAIL_REGEX.matches(trimmedEmail)) {
        errors["email"] = "A valid email address is required"
    }
    if (code.trim().isBlank()) {
        errors["code"] = "Verification code cannot be blank"
    }
    if (newPassword.length < ValidationRules.MIN_PASSWORD_LENGTH) {
        errors["newPassword"] = "New password must be at least ${ValidationRules.MIN_PASSWORD_LENGTH} characters"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [CreateTaskRequest] before saving into the database or propagating sync events.
 *
 * Checks:
 * - [CreateTaskRequest.title]: Non-blank, max length 250 characters
 * - [CreateTaskRequest.endTime]: If supplied, must not be before [CreateTaskRequest.startTime]
 *
 * @throws ValidationException if title is empty or dates are chronologically inverted.
 */
fun CreateTaskRequest.validate() {
    val errors = mutableMapOf<String, String>()

    if (title.trim().isBlank()) {
        errors["title"] = "Task title cannot be blank"
    } else if (title.trim().length > ValidationRules.MAX_TITLE_LENGTH) {
        errors["title"] = "Task title cannot exceed ${ValidationRules.MAX_TITLE_LENGTH} characters"
    }

    if (endTime != null && endTime < startTime) {
        errors["endTime"] = "End time cannot be earlier than start time"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [CreatePlanRoomRequest] ensuring room names are present and length bounded.
 *
 * @throws ValidationException if room name is blank or exceeds 100 characters.
 */
fun CreatePlanRoomRequest.validate() {
    val errors = mutableMapOf<String, String>()

    val trimmedName = name.trim()
    if (trimmedName.isBlank()) {
        errors["name"] = "Room name cannot be blank"
    } else if (trimmedName.length > ValidationRules.MAX_ROOM_NAME_LENGTH) {
        errors["name"] = "Room name cannot exceed ${ValidationRules.MAX_ROOM_NAME_LENGTH} characters"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [RenamePlanRoomRequest] ensuring room names are present and length bounded.
 *
 * @throws ValidationException if room name is blank or exceeds 100 characters.
 */
fun RenamePlanRoomRequest.validate() {
    val errors = mutableMapOf<String, String>()

    val trimmedName = name.trim()
    if (trimmedName.isBlank()) {
        errors["name"] = "Room name cannot be blank"
    } else if (trimmedName.length > ValidationRules.MAX_ROOM_NAME_LENGTH) {
        errors["name"] = "Room name cannot exceed ${ValidationRules.MAX_ROOM_NAME_LENGTH} characters"
    }

    if (errors.isNotEmpty()) {
        throw ValidationException(errors)
    }
}

/**
 * Validates [AutoScheduleRequest] ensuring the list of task IDs is not empty.
 *
 * @throws ValidationException if [AutoScheduleRequest.taskIds] is empty.
 */
fun AutoScheduleRequest.validate() {
    if (taskIds.isEmpty()) {
        throw ValidationException(mapOf("taskIds" to "taskIds list cannot be empty"))
    }
}
