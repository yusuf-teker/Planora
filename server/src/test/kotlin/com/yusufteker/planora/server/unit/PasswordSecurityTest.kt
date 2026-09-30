package com.yusufteker.planora.server.unit

import com.yusufteker.planora.server.security.HashingService
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests verifying password cryptography and hashing security via [HashingService].
 *
 * Confirms BCrypt salt uniqueness, one-way collision resistance, and correct
 * credential validation behavior.
 */
class PasswordSecurityTest {

    /**
     * Verifies that hashing a password produces a valid, non-blank BCrypt formatted string.
     */
    @Test
    fun hashPasswordShouldProduceValidBcryptString() {
        val rawPassword = "mySuperSecretPassword!2026"
        val hash = HashingService.hashPassword(rawPassword)

        assertTrue(hash.isNotBlank(), "Hash should not be blank")
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"),
            "Hash should have BCrypt format identifier")
    }

    /**
     * Verifies BCrypt salting: Hashing the same plaintext password twice MUST produce
     * different hash outputs due to cryptographic random salts, preventing rainbow table attacks.
     */
    @Test
    fun hashingSamePasswordTwiceShouldProduceDifferentHashesDueToSalt() {
        val rawPassword = "consistentPassword123"
        val hash1 = HashingService.hashPassword(rawPassword)
        val hash2 = HashingService.hashPassword(rawPassword)

        assertNotEquals(hash1, hash2, "Each hash invocation must use a unique random salt")
    }

    /**
     * Verifies that [HashingService.verifyPassword] returns true for correct credentials
     * and false for altered, wrong, or blank passwords.
     */
    @Test
    fun verifyPasswordShouldValidateCorrectCredentialsAndRejectIncorrect() {
        val correctPassword = "correctPassword456"
        val hash = HashingService.hashPassword(correctPassword)

        // Doğru şifre başarılı olmalı
        assertTrue(HashingService.verifyPassword(correctPassword, hash), "Correct password must verify")

        // Yanlış şifreler reddedilmeli
        assertFalse(HashingService.verifyPassword("wrongPassword", hash), "Wrong password must be rejected")
        assertFalse(HashingService.verifyPassword("", hash), "Empty password must be rejected")
        assertFalse(HashingService.verifyPassword("correctPassword456 ", hash), "Password with extra whitespace must be rejected")
    }
}
