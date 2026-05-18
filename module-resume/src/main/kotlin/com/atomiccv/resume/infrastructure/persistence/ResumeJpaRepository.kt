package com.atomiccv.resume.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ResumeJpaRepository : JpaRepository<ResumeJpaEntity, Long> {
    fun findBySlug(slug: String): ResumeJpaEntity?

    fun findAllByUserIdAndDeletedAtIsNull(userId: Long): List<ResumeJpaEntity>
}
