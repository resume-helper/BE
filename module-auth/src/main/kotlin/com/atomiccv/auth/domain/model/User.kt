package com.atomiccv.auth.domain.model

import java.time.LocalDateTime

data class User(
    val id: Long = 0,
    val email: String,
    val name: String,
    val profileImageUrl: String? = null,
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val deletedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isWithinGracePeriod(): Boolean =
        !isActive && deletedAt != null && deletedAt.isAfter(LocalDateTime.now().minusDays(30))
}
