package com.atomiccv.auth.infrastructure.client

import com.atomiccv.auth.application.port.RefreshTokenPort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenRedisAdapter(
    private val redisTemplate: StringRedisTemplate,
) : RefreshTokenPort {
    override fun save(
        userId: Long,
        token: String,
        ttl: Duration,
    ) {
        redisTemplate.opsForValue().set(tokenKey(token), userId.toString(), ttl)
        // userId 기반 삭제를 위해 역인덱스도 저장
        redisTemplate.opsForValue().set(userKey(userId), token, ttl)
    }

    override fun findUserIdByToken(token: String): Long? = redisTemplate.opsForValue().get(tokenKey(token))?.toLong()

    override fun deleteByUserId(userId: Long) {
        redisTemplate.delete(userKey(userId))
    }

    private fun tokenKey(token: String) = "refresh:$token"

    private fun userKey(userId: Long) = "refresh-user:$userId"
}
