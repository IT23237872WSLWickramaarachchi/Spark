package com.example.spark.data.local

import java.security.MessageDigest

/**
 * Utility for one-way password hashing using SHA-256.
 *
 * Provides a deterministic hash for password storage and comparison.
 * Pure Kotlin — no Android dependencies, easily unit-testable.
 */
object PasswordHasher {

    /**
     * Returns the SHA-256 hex digest of the given [password].
     */
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
