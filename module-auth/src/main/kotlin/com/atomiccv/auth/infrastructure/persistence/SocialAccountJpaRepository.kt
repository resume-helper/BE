package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.SocialProvider
import org.springframework.data.jpa.repository.JpaRepository

interface SocialAccountJpaRepository : JpaRepository<SocialAccountJpaEntity, Long> {
    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): SocialAccountJpaEntity?

    fun findAllByUserId(userId: Long): List<SocialAccountJpaEntity>

    fun deleteByUserIdIn(userIds: Collection<Long>)
}
