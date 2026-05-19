package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class GetFeedbackListQuery(
    val resumeId: Long,
    val requestUserId: Long,
    val page: Int,
    val size: Int,
)

data class FeedbackListResult(
    val feedbacks: List<Feedback>,
    val totalCount: Long,
)

class GetFeedbackListUseCase(
    private val resumeRepository: ResumeRepository,
    private val feedbackRepository: FeedbackRepository,
) {
    fun getList(query: GetFeedbackListQuery): FeedbackListResult {
        val resume = findResume(query.resumeId)
        if (!resume.isOwnedBy(query.requestUserId)) throw BusinessException(ErrorCode.FORBIDDEN)

        return FeedbackListResult(
            feedbacks = feedbackRepository.findAllByResumeId(query.resumeId, query.page, query.size),
            totalCount = feedbackRepository.countByResumeId(query.resumeId),
        )
    }

    private fun findResume(resumeId: Long): Resume {
        val resume =
            resumeRepository.findById(resumeId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        if (resume.isDeleted()) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        return resume
    }
}
