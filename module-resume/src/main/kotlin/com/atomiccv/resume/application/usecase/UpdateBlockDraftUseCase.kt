package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class UpdateBlockDraftCommand(
    val draftId: Long,
    val userId: Long,
    val title: String,
    val contentJson: String,
)

@Transactional
class UpdateBlockDraftUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun update(command: UpdateBlockDraftCommand): BlockDraft {
        if (command.title.isBlank()) throw BusinessException(ErrorCode.BLOCK_DRAFT_TITLE_REQUIRED)
        val draft = findActiveDraft(command.draftId)
        if (!draft.isOwnedBy(command.userId)) throw BusinessException(ErrorCode.BLOCK_DRAFT_FORBIDDEN)
        return blockDraftRepository.save(
            draft.copy(
                title = command.title,
                contentJson = command.contentJson,
                expiresAt = LocalDateTime.now().plusDays(BlockDraft.EXPIRY_DAYS),
            ),
        )
    }

    private fun findActiveDraft(draftId: Long): BlockDraft {
        val draft =
            blockDraftRepository.findById(draftId)
                ?: throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        if (draft.isExpired()) throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        return draft
    }
}
