package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.model.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true)
    val email: String,
    @Column(nullable = false)
    val name: String,
    @Column(name = "profile_image_url")
    val profileImageUrl: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() =
        User(
            id = id,
            email = email,
            name = name,
            profileImageUrl = profileImageUrl,
            role = role,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun fromDomain(user: User) =
            UserJpaEntity(
                id = user.id,
                email = user.email,
                name = user.name,
                profileImageUrl = user.profileImageUrl,
                role = user.role,
                isActive = user.isActive,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            )
    }
}
