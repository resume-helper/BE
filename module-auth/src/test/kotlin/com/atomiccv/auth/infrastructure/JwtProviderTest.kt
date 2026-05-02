package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.JwtProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtProviderTest {
    // 테스트용 64자 이상 Base64 시크릿 (HS256 최소 256-bit)
    private val secret = "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHktbm90LWZvci1wcm9k"
    private val accessExpiryMs = 3_600_000L // 1시간
    private val provider = JwtProvider(secret, accessExpiryMs)

    @Test
    fun `Access Token 발급 후 userId를 추출할 수 있다`() {
        val token = provider.generateAccessToken(42L)
        assertEquals(42L, provider.extractUserId(token))
    }

    @Test
    fun `유효한 토큰의 validateToken은 true를 반환한다`() {
        val token = provider.generateAccessToken(1L)
        assertTrue(provider.validateToken(token))
    }

    @Test
    fun `위변조된 토큰의 validateToken은 false를 반환한다`() {
        val token = provider.generateAccessToken(1L)
        val tampered = token.dropLast(5) + "XXXXX"
        assertFalse(provider.validateToken(tampered))
    }

    @Test
    fun `만료된 토큰의 validateToken은 false를 반환한다`() {
        val expiredProvider = JwtProvider(secret, -1000L) // 이미 만료
        val token = expiredProvider.generateAccessToken(1L)
        assertFalse(provider.validateToken(token))
    }

    @Test
    fun `getRemainingTtl은 양수 Duration을 반환한다`() {
        val token = provider.generateAccessToken(1L)
        val ttl = provider.getRemainingTtl(token)
        assertTrue(ttl.toMillis() > 0)
    }

    @Test
    fun `extractUserId는 유효하지 않은 토큰에 대해 예외를 발생시킨다`() {
        assertThrows<Exception> { provider.extractUserId("invalid.token.here") }
    }
}
