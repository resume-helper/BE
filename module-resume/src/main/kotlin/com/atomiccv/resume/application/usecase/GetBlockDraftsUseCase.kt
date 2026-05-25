package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import java.time.LocalDateTime

data class GetBlockDraftsQuery(
    val userId: Long,
    val blockType: BlockType,
    val currentDraftId: Long?,
)

data class BlockDraftSummary(
    val id: Long,
    val title: String,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
)

class GetBlockDraftsUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun getDrafts(query: GetBlockDraftsQuery): List<BlockDraftSummary> =
        blockDraftRepository
            .findAllByUserIdAndBlockType(query.userId, query.blockType)
            .filter { !it.isExpired() }
            .map { draft ->
                BlockDraftSummary(
                    id = draft.id,
                    title = draft.title,
                    updatedAt = draft.updatedAt,
                    isActive = draft.id == query.currentDraftId,
                )
            }
}
