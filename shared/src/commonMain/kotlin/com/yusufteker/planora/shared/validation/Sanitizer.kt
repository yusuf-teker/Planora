package com.yusufteker.planora.shared.validation

import com.yusufteker.planora.shared.api.AuthRequest
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.ForgotPasswordRequest
import com.yusufteker.planora.shared.api.RegisterRequest
import com.yusufteker.planora.shared.api.RenamePlanRoomRequest
import com.yusufteker.planora.shared.api.ResetPasswordRequest
import com.yusufteker.planora.shared.api.SendRegisterCodeRequest
import com.yusufteker.planora.shared.api.VerifyResetCodeRequest

/**
 * Utility functions for input sanitization, preventing Stored XSS, whitespace bombing,
 * control character injection, and invisible spoofing characters.
 */
object InputSanitizer {
    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val CONTROL_CHARS_REGEX = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F\\u200B-\\u200D\\uFEFF]")
    private val MULTI_SPACE_REGEX = Regex("[^\\S\\r\\n]+")
    private val EXCESSIVE_NEWLINES_REGEX = Regex("(\\r?\\n){3,}")

    /**
     * Sanitizes a string input by:
     * 1. Stripping all HTML / XML markup tags.
     * 2. Removing non-printable control characters and zero-width spaces.
     * 3. Normalizing line endings (\r\n -> \n).
     * 4. Collapsing excessive consecutive newlines and spaces.
     * 5. Trimming leading and trailing whitespace.
     *
     * @param input Raw string to sanitize
     * @param allowNewlines If false, all line breaks are replaced with spaces (for single-line fields)
     * @return Cleaned and normalized string
     */
    fun sanitize(input: String, allowNewlines: Boolean = true): String {
        if (input.isEmpty()) return input

        var text = input
            .replace(HTML_TAG_REGEX, "")
            .replace(CONTROL_CHARS_REGEX, "")

        if (!allowNewlines) {
            text = text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ')
            text = text.replace(MULTI_SPACE_REGEX, " ")
        } else {
            text = text.replace("\r\n", "\n").replace('\r', '\n')
            text = text.replace(EXCESSIVE_NEWLINES_REGEX, "\n\n")
            text = text.replace(MULTI_SPACE_REGEX, " ")
        }

        return text.trim()
    }
}

/**
 * Sanitizes a single-line text field (e.g. titles, names, usernames).
 * Strips HTML, control characters, tabs, newlines, and trims excess spaces.
 */
fun String.sanitizeSingleLine(): String = InputSanitizer.sanitize(this, allowNewlines = false)

/**
 * Sanitizes a multi-line text field (e.g. descriptions, notes, post contents).
 * Strips HTML, control characters, collapses excessive blank lines, and trims outer whitespace.
 */
fun String.sanitizeMultiLine(): String = InputSanitizer.sanitize(this, allowNewlines = true)

/**
 * Sanitizes [RegisterRequest] fields before validation and persistence.
 *
 * @return A new [RegisterRequest] with sanitized [name], [username], and [email].
 */
fun RegisterRequest.sanitize(): RegisterRequest = copy(
    name = name.sanitizeSingleLine(),
    username = username.sanitizeSingleLine().lowercase(),
    email = email.sanitizeSingleLine().lowercase(),
    code = code.trim()
)

/**
 * Sanitizes [AuthRequest] fields before validation and querying.
 *
 * @return A new [AuthRequest] with cleaned [identifier].
 */
fun AuthRequest.sanitize(): AuthRequest = copy(
    identifier = identifier.sanitizeSingleLine()
)

/**
 * Sanitizes [SendRegisterCodeRequest] fields before validation.
 *
 * @return A new [SendRegisterCodeRequest] with trimmed and sanitized [email] and [username].
 */
fun SendRegisterCodeRequest.sanitize(): SendRegisterCodeRequest = copy(
    email = email.sanitizeSingleLine().lowercase(),
    username = username.sanitizeSingleLine().lowercase()
)

/**
 * Sanitizes [ForgotPasswordRequest] fields.
 */
fun ForgotPasswordRequest.sanitize(): ForgotPasswordRequest = copy(
    email = email.sanitizeSingleLine().lowercase()
)

/**
 * Sanitizes [VerifyResetCodeRequest] fields.
 */
fun VerifyResetCodeRequest.sanitize(): VerifyResetCodeRequest = copy(
    email = email.sanitizeSingleLine().lowercase(),
    code = code.trim()
)

/**
 * Sanitizes [ResetPasswordRequest] fields.
 */
fun ResetPasswordRequest.sanitize(): ResetPasswordRequest = copy(
    email = email.sanitizeSingleLine().lowercase(),
    code = code.trim()
)

/**
 * Sanitizes [CreateTaskRequest] fields, preventing script tags and whitespace bombing in tasks.
 *
 * @return A new [CreateTaskRequest] with clean [title], [description], and [tags].
 */
fun CreateTaskRequest.sanitize(): CreateTaskRequest = copy(
    title = title.sanitizeSingleLine(),
    description = description?.sanitizeMultiLine()?.takeIf { it.isNotEmpty() },
    tags = tags.map { it.sanitizeSingleLine() }.filter { it.isNotEmpty() }
)

/**
 * Sanitizes [CreatePlanRoomRequest] name.
 */
fun CreatePlanRoomRequest.sanitize(): CreatePlanRoomRequest = copy(
    name = name.sanitizeSingleLine()
)

/**
 * Sanitizes [RenamePlanRoomRequest] name.
 */
fun RenamePlanRoomRequest.sanitize(): RenamePlanRoomRequest = copy(
    name = name.sanitizeSingleLine()
)
