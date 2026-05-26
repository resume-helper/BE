package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class DeleteBlockDraftsCommand(
    val draftIds: List<Long>,
    val userId: Long,
)

@Transactional
class DeleteBlockDraftsUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun deleteAll(command: DeleteBlockDraftsCommand) {
        if (command.draftIds.isEmpty()) return
        val drafts = blockDraftRepository.findAllByIds(command.draftIds)
        if (drafts.any { !it.isOwnedBy(command.userId) }) {
            throw BusinessException(ErrorCode.BLOCK_DRAFT_FORBIDDEN)
        }
        blockDraftRepository.deleteAllByIds(command.draftIds)
    }
}
