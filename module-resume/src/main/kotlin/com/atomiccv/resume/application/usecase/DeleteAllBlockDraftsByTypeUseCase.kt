package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import org.springframework.transaction.annotation.Transactional

data class DeleteAllBlockDraftsByTypeCommand(
    val userId: Long,
    val blockType: BlockType,
)

@Transactional
class DeleteAllBlockDraftsByTypeUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun deleteAll(command: DeleteAllBlockDraftsByTypeCommand) {
        blockDraftRepository.deleteAllByUserIdAndBlockType(command.userId, command.blockType)
    }
}
