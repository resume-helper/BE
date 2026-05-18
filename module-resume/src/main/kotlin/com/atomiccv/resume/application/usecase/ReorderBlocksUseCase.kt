package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.ResumeBlock
import com.atomiccv.resume.domain.repository.ResumeBlockRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class ReorderBlocksCommand(
    val resumeId: Long,
    val userId: Long,
    val blockIds: List<Long>,
)

@Transactional
class ReorderBlocksUseCase(
    private val resumeRepository: ResumeRepository,
    private val resumeBlockRepository: ResumeBlockRepository,
) {
    fun reorder(command: ReorderBlocksCommand) {
        val resume =
            resumeRepository.findById(command.resumeId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        if (resume.isDeleted()) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        if (!resume.isOwnedBy(command.userId)) throw BusinessException(ErrorCode.FORBIDDEN)

        val existingBlockIds = resumeBlockRepository.findAllByResumeId(command.resumeId).map { it.blockId }.toSet()
        val requestedIds = command.blockIds.toSet()

        if (command.blockIds.size != requestedIds.size) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "블록 ID가 중복되었습니다.")
        }
        if (requestedIds != existingBlockIds) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "블록 목록이 이력서의 블록 목록과 일치하지 않습니다.")
        }

        resumeBlockRepository.deleteAllByResumeId(command.resumeId)
        resumeBlockRepository.saveAll(
            command.blockIds.mapIndexed { index, blockId ->
                ResumeBlock(resumeId = command.resumeId, blockId = blockId, orderIndex = index)
            },
        )
    }
}
