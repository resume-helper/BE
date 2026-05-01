package com.atomiccv.auth.domain.repository

import com.atomiccv.auth.domain.model.User

interface UserRepository {
    fun save(user: User): User

    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean
}
