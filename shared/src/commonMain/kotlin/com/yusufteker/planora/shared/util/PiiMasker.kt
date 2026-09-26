package com.yusufteker.planora.shared.util

/**
 * Utility functions for Personally Identifiable Information (PII) masking.
 *
 * Ensures sensitive data such as emails, passwords, authentication tokens,
 * phone numbers, and query parameters are never written in plain text across
 * console logs, crash analytics, or server access traces.
 */
object PiiMasker {

    private val SENSITIVE_QUERY_PARAM_REGEX = Regex(
        "(?i)([?&](password|token|code|secret|api_?key|email|identifier|auth|verification_?code)=)([^&]+)"
    )

    /**
     * Masks an email address preserving only the initial and last character of the username
     * and the full domain (e.g. "yusufteker@gmail.com" -> "y***r@gmail.com").
     *
     * @param email The raw email address string to mask.
     * @return The masked email, or an empty string / fallback if invalid.
     */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return ""
        val atIndex = email.indexOf('@')
        if (atIndex <= 0 || atIndex >= email.length - 1) return "***"

        val username = email.substring(0, atIndex)
        val domain = email.substring(atIndex + 1)

        val maskedUser = when {
            username.length <= 2 -> "${username.first()}***"
            else -> "${username.first()}***${username.last()}"
        }
        return "$maskedUser@$domain"
    }

    /**
     * Masks any secret, password, or OTP code into a fixed mask representation.
     *
     * @param secret The secret text to mask.
     * @return Constant masked string "******".
     */
    fun maskSecret(secret: String?): String {
        if (secret.isNullOrBlank()) return ""
        return "******"
    }

    /**
     * Masks an authentication token or secret key by keeping only prefix and suffix.
     * (e.g. "eyJhbGciOi...abcd" -> "eyJh***abcd").
     *
     * @param token The token or API key to mask.
     * @param visibleChars Number of visible prefix and suffix characters.
     * @return Truncated and masked token representation.
     */
    fun maskToken(token: String?, visibleChars: Int = 4): String {
        if (token.isNullOrBlank()) return ""
        if (token.length <= visibleChars * 2) return "******"
        return "${token.take(visibleChars)}***${token.takeLast(visibleChars)}"
    }

    /**
     * Masks sensitive query parameters within a URI string (e.g. /auth/verify?email=a@b.com&code=123456).
     * Replaces sensitive values with "***".
     *
     * @param uri The incoming request URI with optional query parameters.
     * @return The sanitized URI with all sensitive parameters masked.
     */
    fun maskUri(uri: String?): String {
        if (uri.isNullOrBlank()) return ""
        return uri.replace(SENSITIVE_QUERY_PARAM_REGEX, "$1***")
    }
}

/**
 * Extension helper to mask an email string.
 */
fun String.maskEmail(): String = PiiMasker.maskEmail(this)

/**
 * Extension helper to mask a secret or password string.
 */
fun String.maskSecret(): String = PiiMasker.maskSecret(this)

/**
 * Extension helper to mask an access or refresh token string.
 */
fun String.maskToken(visibleChars: Int = 4): String = PiiMasker.maskToken(this, visibleChars)

/**
 * Extension helper to mask sensitive query parameters in a request URI.
 */
fun String.maskUri(): String = PiiMasker.maskUri(this)
