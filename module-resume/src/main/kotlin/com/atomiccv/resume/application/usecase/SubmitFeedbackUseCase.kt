package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class SubmitFeedbackCommand(
    val resumeId: Long,
    val rating: Int,
    val comment: String?,
    val tags: List<String>,
    val reviewerIp: String,
    val requestUserId: Long? = null,
)

@Transactional
class SubmitFeedbackUseCase(
    private val resumeRepository: ResumeRepository,
    private val feedbackRepository: FeedbackRepository,
) {
    fun submit(command: SubmitFeedbackCommand): Feedback {
        val resume = findValidResume(command.resumeId)
        if (command.requestUserId != null && resume.isOwnedBy(command.requestUserId)) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        return feedbackRepository.save(
            Feedback(
                resumeId = command.resumeId,
                rating = command.rating,
                comment = command.comment,
                reviewerIp = command.reviewerIp,
                tags = command.tags,
            ),
        )
    }

    private fun findValidResume(resumeId: Long): Resume {
        val resume =
            resumeRepository.findById(resumeId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        if (resume.isDeleted() || !resume.isPublic) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        }
        return resume
    }
}
