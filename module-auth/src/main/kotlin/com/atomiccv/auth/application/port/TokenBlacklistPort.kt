package com.atomiccv.auth.application.port

import java.time.Duration

interface TokenBlacklistPort {
    fun add(
        token: String,
        ttl: Duration,
    )

    fun isBlacklisted(token: String): Boolean
}
