package com.yusufteker.pulse.server.security

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Handles password hashing and verification using the BCrypt algorithm.
 * BCrypt automatically generates a salt for each password and stores it alongside the hash,
 * preventing rainbow table attacks and making brute-force attacks extremely slow.
 */
object HashingService {
    
    /**
     * Hashes a raw password with a cost factor of 12.
     */
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    /**
     * Verifies a raw password against an existing BCrypt hash.
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}
