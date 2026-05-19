package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
class DeleteResumeUseCase(
    private val resumeRepository: ResumeRepository,
) {
    fun delete(
        resumeId: Long,
        userId: Long
    ) {
        val resume = findActiveResume(resumeId)
        verifyOwnership(resume, userId)
        resumeRepository.save(resume.copy(deletedAt = LocalDateTime.now()))
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
