package com.atomiccv.auth.infrastructure.client

import com.atomiccv.auth.application.port.TokenBlacklistPort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TokenBlacklistRedisAdapter(
    private val redisTemplate: StringRedisTemplate,
) : TokenBlacklistPort {
    override fun add(
        token: String,
        ttl: Duration,
    ) {
        redisTemplate.opsForValue().set(key(token), "1", ttl)
    }

    override fun isBlacklisted(token: String): Boolean = redisTemplate.hasKey(key(token)) == true

    private fun key(token: String) = "blacklist:$token"
}
