package com.example.spark.domain.model

import com.example.spark.data.local.entity.UserEntity
import java.time.Instant

/**
 * Domain model representing a user account.
 */
data class User(
    val id: String = "",
    val name: String,
    val email: String,
    val passwordHash: String = "",
    val createdAt: Instant = Instant.now()
)

fun UserEntity.toDomain(): User = User(
    id = id.toString(),
    name = name,
    email = email,
    passwordHash = passwordHash,
    createdAt = createdAt
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id.toLongOrNull() ?: 0L,
    name = name,
    email = email,
    passwordHash = passwordHash,
    createdAt = createdAt
)
