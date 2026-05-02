package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.TokenBlacklistRedisAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenBlacklistRedisAdapterTest {
    private val redisTemplate: StringRedisTemplate = mockk()
    private val valueOps: ValueOperations<String, String> = mockk()
    private val adapter = TokenBlacklistRedisAdapter(redisTemplate)

    init {
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @Test
    fun `Blacklist에 토큰을 추가하면 'blacklist-{token}' 키로 저장된다`() {
        every { valueOps.set("blacklist:my-token", "1", Duration.ofHours(1)) } returns Unit

        adapter.add("my-token", Duration.ofHours(1))

        verify { valueOps.set("blacklist:my-token", "1", Duration.ofHours(1)) }
    }

    @Test
    fun `Blacklist에 있는 토큰은 isBlacklisted가 true를 반환한다`() {
        every { redisTemplate.hasKey("blacklist:my-token") } returns true

        assertTrue(adapter.isBlacklisted("my-token"))
    }

    @Test
    fun `Blacklist에 없는 토큰은 isBlacklisted가 false를 반환한다`() {
        every { redisTemplate.hasKey("blacklist:missing") } returns false

        assertFalse(adapter.isBlacklisted("missing"))
    }
}
