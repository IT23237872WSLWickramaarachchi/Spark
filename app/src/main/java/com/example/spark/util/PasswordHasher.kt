package com.example.spark.util

import java.security.MessageDigest

object PasswordHasher {

    private const val SALT = "SPARK_OFFLINE_SALT_SE4041"

    fun hashPassword(password: String): String {
        val saltedPassword = "$SALT$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(saltedPassword.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val calculatedHash = hashPassword(password)
        return calculatedHash == storedHash
    }
}
