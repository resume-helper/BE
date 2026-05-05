package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.SocialProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface SocialAccountJpaRepository : JpaRepository<SocialAccountJpaEntity, Long> {
    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): SocialAccountJpaEntity?

    fun findAllByUserId(userId: Long): List<SocialAccountJpaEntity>

    fun findByUserIdAndProvider(
        userId: Long,
        provider: SocialProvider
    ): SocialAccountJpaEntity?

    fun countByUserIdAndIsActiveTrue(userId: Long): Int

    fun findByDeletedAtBefore(cutoff: LocalDateTime): List<SocialAccountJpaEntity>

    fun deleteByUserIdIn(userIds: Collection<Long>)
}
