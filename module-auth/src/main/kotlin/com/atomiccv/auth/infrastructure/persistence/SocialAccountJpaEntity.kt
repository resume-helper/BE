package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_user_id"])],
)
class SocialAccountJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: SocialProvider,
    @Column(name = "provider_user_id", nullable = false)
    val providerUserId: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() =
        SocialAccount(
            id = id,
            userId = userId,
            provider = provider,
            providerUserId = providerUserId,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(account: SocialAccount) =
            SocialAccountJpaEntity(
                id = account.id,
                userId = account.userId,
                provider = account.provider,
                providerUserId = account.providerUserId,
                createdAt = account.createdAt,
            )
    }
}
