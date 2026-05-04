package com.atomiccv.auth.application.port

import java.time.Duration

interface RefreshTokenPort {
    fun save(
        userId: Long,
        token: String,
        ttl: Duration,
    )

    fun findUserIdByToken(token: String): Long?

    fun deleteByUserId(userId: Long)
}
