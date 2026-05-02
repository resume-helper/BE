package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import org.springframework.stereotype.Repository

@Repository
class SocialAccountRepositoryImpl(
    private val jpaRepository: SocialAccountJpaRepository,
) : SocialAccountRepository {
    override fun save(socialAccount: SocialAccount): SocialAccount =
        jpaRepository.save(SocialAccountJpaEntity.fromDomain(socialAccount)).toDomain()

    override fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): SocialAccount? = jpaRepository.findByProviderAndProviderUserId(provider, providerUserId)?.toDomain()

    override fun findAllByUserId(userId: Long): List<SocialAccount> =
        jpaRepository.findAllByUserId(userId).map { it.toDomain() }
}
