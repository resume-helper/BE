package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class UpdateResumeVisibilityCommand(
    val resumeId: Long,
    val userId: Long,
    val isPublic: Boolean,
)

@Transactional
class UpdateResumeVisibilityUseCase(
    private val resumeRepository: ResumeRepository,
) {
    fun update(command: UpdateResumeVisibilityCommand): Resume {
        val resume = findActiveResume(command.resumeId)
        verifyOwnership(resume, command.userId)
        return resumeRepository.save(resume.copy(isPublic = command.isPublic))
    }

    private fun findActiveResume(id: Long): Resume {
        val resume =
            resumeRepository.findById(id)
                ?: throw BusinessException(ErrorCode.RESUME_NOT_FOUND)
        if (resume.isDeleted()) throw BusinessException(ErrorCode.RESUME_NOT_FOUND)
        return resume
    }

    private fun verifyOwnership(
        resume: Resume,
        userId: Long
    ) {
        if (!resume.isOwnedBy(userId)) throw BusinessException(ErrorCode.FORBIDDEN)
    }
}
