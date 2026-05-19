package com.atomiccv.resume.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackTagJpaRepository : JpaRepository<FeedbackTagJpaEntity, Long> {
    fun findAllByFeedbackId(feedbackId: Long): List<FeedbackTagJpaEntity>

    fun findAllByFeedbackIdIn(feedbackIds: List<Long>): List<FeedbackTagJpaEntity>

    fun deleteAllByFeedbackId(feedbackId: Long)
}
