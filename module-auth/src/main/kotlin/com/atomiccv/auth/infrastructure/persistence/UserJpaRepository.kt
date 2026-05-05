package com.atomiccv.auth.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByEmail(email: String): UserJpaEntity?

    fun existsByEmail(email: String): Boolean

    fun findByDeletedAtBefore(cutoff: LocalDateTime): List<UserJpaEntity>
}
