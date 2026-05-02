package com.atomiccv.auth.infrastructure.client

import com.atomiccv.auth.application.port.JwtPort
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-expiry-ms}") private val accessExpiryMs: Long,
) : JwtPort {
    private val key: SecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    override fun generateAccessToken(userId: Long): String {
        val now = Date()
        return Jwts
            .builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + accessExpiryMs))
            .signWith(key)
            .compact()
    }

    override fun validateToken(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    override fun extractUserId(token: String): Long = parseClaims(token).subject.toLong()

    override fun getExpiration(token: String): Date = parseClaims(token).expiration

    override fun getRemainingTtl(token: String): Duration {
        val expiration = getExpiration(token)
        val remaining = expiration.time - System.currentTimeMillis()
        return if (remaining > 0) Duration.ofMillis(remaining) else Duration.ZERO
    }

    private fun parseClaims(token: String) =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
