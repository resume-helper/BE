package com.atomiccv.resume.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ResumeBlockJpaRepository : JpaRepository<ResumeBlockJpaEntity, Long> {
    fun findAllByResumeId(resumeId: Long): List<ResumeBlockJpaEntity>

    fun deleteAllByResumeId(resumeId: Long)
}
