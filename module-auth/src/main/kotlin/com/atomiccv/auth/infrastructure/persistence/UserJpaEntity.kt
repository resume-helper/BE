package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.model.UserRole
import com.atomiccv.shared.infrastructure.persistence.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

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
) : BaseJpaEntity() {
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
            )
    }
}
