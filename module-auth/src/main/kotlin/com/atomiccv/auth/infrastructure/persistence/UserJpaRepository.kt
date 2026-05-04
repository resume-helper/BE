package com.atomiccv.auth.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByEmail(email: String): UserJpaEntity?

    fun existsByEmail(email: String): Boolean
}
