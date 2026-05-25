package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class DeleteBlockDraftCommand(
    val draftId: Long,
    val userId: Long,
)

@Transactional
class DeleteBlockDraftUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun delete(command: DeleteBlockDraftCommand) {
        val draft =
            blockDraftRepository.findById(command.draftId)
                ?: throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        if (!draft.isOwnedBy(command.userId)) throw BusinessException(ErrorCode.BLOCK_DRAFT_FORBIDDEN)
        blockDraftRepository.deleteById(command.draftId)
    }
}
