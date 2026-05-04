package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): User = jpaRepository.save(UserJpaEntity.fromDomain(user)).toDomain()

    override fun findById(id: Long): User? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? = jpaRepository.findByEmail(email)?.toDomain()

    override fun existsByEmail(email: String): Boolean = jpaRepository.existsByEmail(email)
}
