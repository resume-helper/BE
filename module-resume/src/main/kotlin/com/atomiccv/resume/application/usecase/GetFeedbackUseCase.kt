package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class GetFeedbackQuery(
    val resumeId: Long,
    val feedbackId: Long,
    val requestUserId: Long,
)

class GetFeedbackUseCase(
    private val resumeRepository: ResumeRepository,
    private val feedbackRepository: FeedbackRepository,
) {
    fun get(query: GetFeedbackQuery): Feedback {
        val resume = findResume(query.resumeId)
        if (!resume.isOwnedBy(query.requestUserId)) throw BusinessException(ErrorCode.FORBIDDEN)
        return findFeedbackForResume(query.resumeId, query.feedbackId)
    }

    private fun findResume(resumeId: Long): Resume {
        val resume =
            resumeRepository.findById(resumeId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        if (resume.isDeleted()) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        return resume
    }

    private fun findFeedbackForResume(
        resumeId: Long,
        feedbackId: Long,
    ): Feedback {
        val feedback =
            feedbackRepository.findById(feedbackId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "피드백을 찾을 수 없습니다.")
        if (feedback.resumeId != resumeId) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "피드백을 찾을 수 없습니다.")
        return feedback
    }
}
