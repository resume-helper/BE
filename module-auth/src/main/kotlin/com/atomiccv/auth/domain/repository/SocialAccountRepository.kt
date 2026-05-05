package com.atomiccv.auth.domain.repository

import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider

interface SocialAccountRepository {
    fun save(socialAccount: SocialAccount): SocialAccount

    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): SocialAccount?

    fun findByUserIdAndProvider(
        userId: Long,
        provider: SocialProvider
    ): SocialAccount?

    fun findAllByUserId(userId: Long): List<SocialAccount>

    fun countActiveByUserId(userId: Long): Int
}
