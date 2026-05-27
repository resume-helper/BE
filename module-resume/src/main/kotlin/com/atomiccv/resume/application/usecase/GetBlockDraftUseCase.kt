package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class GetBlockDraftQuery(
    val draftId: Long,
    val userId: Long,
)

class GetBlockDraftUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun getDraft(query: GetBlockDraftQuery): BlockDraft {
        val draft =
            blockDraftRepository.findById(query.draftId)
                ?: throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        validateDraft(draft, query.userId)
        return draft
    }

    private fun validateDraft(
        draft: BlockDraft,
        userId: Long
    ) {
        if (draft.isExpired()) throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        if (!draft.isOwnedBy(userId)) throw BusinessException(ErrorCode.BLOCK_DRAFT_FORBIDDEN)
    }
}
