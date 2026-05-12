package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository

data class GetBlocksQuery(
    val userId: Long,
    val type: BlockType? = null,
)

class GetBlocksUseCase(
    private val blockRepository: BlockRepository,
) {
    fun getBlocks(query: GetBlocksQuery): List<Block> =
        if (query.type != null) {
            blockRepository.findAllActiveByUserIdAndType(query.userId, query.type)
        } else {
            blockRepository.findAllActiveByUserId(query.userId)
        }
}
