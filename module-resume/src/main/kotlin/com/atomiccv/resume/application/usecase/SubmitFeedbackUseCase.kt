package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.model.FeedbackTagCatalog
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class SubmitFeedbackCommand(
    val resumeId: Long,
    /** 피드백 대상 섹션. null = 이력서 전체 피드백 */
    val section: BlockType? = null,
    /** 별점 1.0~5.0, 0.5 단위. 선택 항목 */
    val rating: Double?,
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
        validateContent(command)
        validateRating(command.rating)
        validateSection(command)
        validateTags(command)
        return feedbackRepository.save(
            Feedback(
                resumeId = command.resumeId,
                section = command.section,
                rating = command.rating,
                comment = command.comment,
                reviewerIp = command.reviewerIp,
                tags = command.tags,
            ),
        )
    }

    private fun validateContent(command: SubmitFeedbackCommand) {
        val hasContent =
            command.rating != null || command.tags.isNotEmpty() || !command.comment.isNullOrBlank()
        if (!hasContent) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "별점·태그·텍스트 중 하나 이상을 입력해주세요.")
        }
    }

    private fun validateRating(rating: Double?) {
        if (rating == null) return
        val isHalfStep = (rating * 2) % 1.0 == 0.0
        if (rating < MIN_RATING || rating > MAX_RATING || !isHalfStep) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "별점은 1.0~5.0 사이 0.5 단위여야 합니다.")
        }
    }

    private fun validateSection(command: SubmitFeedbackCommand) {
        val section = command.section ?: return
        if (!FeedbackTagCatalog.isFeedbackSection(section)) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "피드백을 남길 수 없는 섹션입니다.")
        }
        if (!command.comment.isNullOrBlank()) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "섹션 피드백에는 텍스트를 남길 수 없습니다.")
        }
    }

    private fun validateTags(command: SubmitFeedbackCommand) {
        val allowed = FeedbackTagCatalog.allowedTags(command.section)
        val invalid = command.tags.filterNot { it in allowed }
        if (invalid.isNotEmpty()) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "허용되지 않은 태그입니다: ${invalid.joinToString()}")
        }
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

    companion object {
        private const val MIN_RATING = 1.0
        private const val MAX_RATING = 5.0
    }
}
