package com.atomiccv.auth.application.port

import java.time.Duration
import java.util.Date

interface JwtPort {
    fun generateAccessToken(userId: Long): String

    fun validateToken(token: String): Boolean

    fun extractUserId(token: String): Long

    fun getExpiration(token: String): Date

    fun getRemainingTtl(token: String): Duration
}
