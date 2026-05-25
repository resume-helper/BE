package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class CreateBlockDraftCommand(
    val userId: Long,
    val blockType: BlockType,
    val title: String,
    val contentJson: String,
)

@Transactional
class CreateBlockDraftUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun create(command: CreateBlockDraftCommand): BlockDraft {
        if (command.title.isBlank()) throw BusinessException(ErrorCode.BLOCK_DRAFT_TITLE_REQUIRED)
        return blockDraftRepository.save(
            BlockDraft(
                userId = command.userId,
                blockType = command.blockType,
                title = command.title,
                contentJson = command.contentJson,
                expiresAt = LocalDateTime.now().plusDays(BlockDraft.EXPIRY_DAYS),
            ),
        )
    }
}
