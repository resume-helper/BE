package com.atomiccv.auth.domain.model

import java.time.LocalDateTime

data class User(
    val id: Long = 0,
    val email: String,
    val name: String,
    val password: String? = null,
    val profileImageUrl: String? = null,
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
)
