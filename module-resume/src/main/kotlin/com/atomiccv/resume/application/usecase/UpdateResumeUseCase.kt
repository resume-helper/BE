package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeBlock
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class UpdateResumeCommand(
    val resumeId: Long,
    val userId: Long,
    val title: String,
    val blocks: List<ResumeBlockInput>,
)

@Transactional
class UpdateResumeUseCase(
    private val resumeRepository: ResumeRepository,
) {
    fun update(command: UpdateResumeCommand): Resume {
        val resume = findActiveResume(command.resumeId)
        verifyOwnership(resume, command.userId)
        val updated = resumeRepository.save(resume.copy(title = command.title, updatedAt = LocalDateTime.now()))
        replaceBlocks(command.resumeId, command.blocks)
        return updated
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

    private fun replaceBlocks(
        resumeId: Long,
        blocks: List<ResumeBlockInput>
    ) {
        resumeRepository.deleteBlocksByResumeId(resumeId)
        blocks.forEach { input ->
            resumeRepository.saveBlock(
                ResumeBlock(resumeId = resumeId, blockId = input.blockId, orderIndex = input.orderIndex),
            )
        }
    }
}
