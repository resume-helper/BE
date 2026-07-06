package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository

data class BlockTypeCount(
    val type: BlockType,
    val count: Long,
)

data class BlockCounts(
    val totalCount: Long,
    val counts: List<BlockTypeCount>,
)

class GetBlockCountsUseCase(
    private val blockRepository: BlockRepository,
) {
    fun getCounts(userId: Long): BlockCounts {
        val countsByType = blockRepository.countByUserId(userId)
        val counts = BlockType.entries.map { type -> BlockTypeCount(type, countsByType[type] ?: 0L) }
        return BlockCounts(totalCount = counts.sumOf { it.count }, counts = counts)
    }
}
