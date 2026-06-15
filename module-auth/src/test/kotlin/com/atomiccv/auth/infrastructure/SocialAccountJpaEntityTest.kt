package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.infrastructure.persistence.SocialAccountJpaEntity
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class SocialAccountJpaEntityTest {
    @Test
    fun `fromDomain은 도메인 createdAt을 보존하고 toDomain으로 round-trip 가능하다`() {
        val createdAt = LocalDateTime.now().minusDays(3)
        val deletedAt = LocalDateTime.now()
        val domain =
            SocialAccount(
                id = 1L,
                userId = 10L,
                provider = SocialProvider.GOOGLE,
                providerUserId = "g-1",
                isActive = false,
                deletedAt = deletedAt,
                createdAt = createdAt,
            )

        val roundTripped = SocialAccountJpaEntity.fromDomain(domain).toDomain()

        assertEquals(domain.id, roundTripped.id)
        assertEquals(domain.userId, roundTripped.userId)
        assertEquals(domain.provider, roundTripped.provider)
        assertEquals(domain.providerUserId, roundTripped.providerUserId)
        assertEquals(domain.isActive, roundTripped.isActive)
        assertEquals(domain.deletedAt, roundTripped.deletedAt)
        assertEquals(domain.createdAt, roundTripped.createdAt)
    }
}
