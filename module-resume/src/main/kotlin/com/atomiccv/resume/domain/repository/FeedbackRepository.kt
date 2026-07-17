package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.Feedback

interface FeedbackRepository {
    fun save(feedback: Feedback): Feedback

    fun findById(id: Long): Feedback?

    fun findAllByResumeId(
        resumeId: Long,
        page: Int,
        size: Int,
    ): List<Feedback>

    /** 통계 집계용 전체 조회 (페이징 없음) */
    fun findAllByResumeId(resumeId: Long): List<Feedback>

    fun countByResumeId(resumeId: Long): Long

    fun findAllByResumeIdIn(
        resumeIds: List<Long>,
        page: Int,
        size: Int,
    ): List<Feedback>

    fun countByResumeIdIn(resumeIds: List<Long>): Long

    fun deleteById(feedbackId: Long)
}
