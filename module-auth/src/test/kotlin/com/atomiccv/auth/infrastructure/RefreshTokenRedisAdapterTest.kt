package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.RefreshTokenRedisAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RefreshTokenRedisAdapterTest {
    private val redisTemplate: StringRedisTemplate = mockk()
    private val valueOps: ValueOperations<String, String> = mockk()
    private val adapter = RefreshTokenRedisAdapter(redisTemplate)

    init {
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @Test
    fun `Refresh Token을 저장하면 'refresh-{token}' 키로 userId를 저장한다`() {
        every { valueOps.set("refresh:my-token", "10", Duration.ofDays(7)) } returns Unit
        every { valueOps.set("refresh-user:10", "my-token", Duration.ofDays(7)) } returns Unit

        adapter.save(10L, "my-token", Duration.ofDays(7))

        verify { valueOps.set("refresh:my-token", "10", Duration.ofDays(7)) }
    }

    @Test
    fun `저장된 Refresh Token으로 userId를 조회할 수 있다`() {
        every { valueOps.get("refresh:my-token") } returns "10"

        val userId = adapter.findUserIdByToken("my-token")

        assertEquals(10L, userId)
    }

    @Test
    fun `존재하지 않는 Refresh Token 조회 시 null을 반환한다`() {
        every { valueOps.get("refresh:missing") } returns null

        assertNull(adapter.findUserIdByToken("missing"))
    }

    @Test
    fun `deleteByUserId는 해당 userId 값을 가진 키를 삭제한다`() {
        every { redisTemplate.delete("refresh-user:10") } returns true

        adapter.deleteByUserId(10L)

        verify { redisTemplate.delete("refresh-user:10") }
    }
}
