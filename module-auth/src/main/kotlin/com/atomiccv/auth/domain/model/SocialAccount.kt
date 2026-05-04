package com.atomiccv.auth.domain.model

import java.time.LocalDateTime

data class SocialAccount(
    val id: Long = 0,
    val userId: Long,
    val provider: SocialProvider,
    val providerUserId: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
