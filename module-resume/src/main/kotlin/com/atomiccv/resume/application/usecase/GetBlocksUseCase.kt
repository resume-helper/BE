package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.data.domain.Page

data class GetBlocksQuery(
    val userId: Long,
    val type: BlockType? = null,
    val page: Int = 1,
    val size: Int = 20,
)

class GetBlocksUseCase(
    private val blockRepository: BlockRepository,
) {
    fun getBlocks(query: GetBlocksQuery): Page<Block> =
        if (query.type != null) {
            blockRepository.findPageByUserIdAndType(query.userId, query.type, query.page - 1, query.size)
        } else {
            blockRepository.findPageByUserId(query.userId, query.page - 1, query.size)
        }
}
