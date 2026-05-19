package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository

data class GetAllFeedbacksQuery(
    val requestUserId: Long,
    val page: Int,
    val size: Int,
)

class GetAllFeedbacksUseCase(
    private val resumeRepository: ResumeRepository,
    private val feedbackRepository: FeedbackRepository,
) {
    fun getAll(query: GetAllFeedbacksQuery): FeedbackListResult {
        val resumeIds =
            resumeRepository
                .findAllByUserId(query.requestUserId)
                .filter { !it.isDeleted() }
                .map { it.id }
        return FeedbackListResult(
            feedbacks = feedbackRepository.findAllByResumeIdIn(resumeIds, query.page, query.size),
            totalCount = feedbackRepository.countByResumeIdIn(resumeIds),
        )
    }
}
