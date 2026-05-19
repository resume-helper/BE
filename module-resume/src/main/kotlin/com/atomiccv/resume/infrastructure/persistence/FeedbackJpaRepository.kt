package com.atomiccv.resume.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackJpaRepository : JpaRepository<FeedbackJpaEntity, Long> {
    fun findAllByResumeId(
        resumeId: Long,
        pageable: Pageable,
    ): Page<FeedbackJpaEntity>

    fun countByResumeId(resumeId: Long): Long

    fun findAllByResumeIdIn(
        resumeIds: List<Long>,
        pageable: Pageable,
    ): Page<FeedbackJpaEntity>

    fun countByResumeIdIn(resumeIds: List<Long>): Long
}
