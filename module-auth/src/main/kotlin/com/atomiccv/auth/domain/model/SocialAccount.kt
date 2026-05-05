package com.atomiccv.auth.domain.model

import java.time.LocalDateTime

data class SocialAccount(
    val id: Long = 0,
    val userId: Long,
    val provider: SocialProvider,
    val providerUserId: String,
    val isActive: Boolean = true,
    val deletedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isWithinGracePeriod(): Boolean =
        !isActive && deletedAt != null && deletedAt.isAfter(LocalDateTime.now().minusDays(30))
}
