package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.domain.Page

interface BlockRepository {
    fun save(block: Block): Block

    fun saveAll(blocks: List<Block>): List<Block>

    fun findById(id: Long): Block?

    fun findPageByUserId(
        userId: Long,
        page: Int,
        size: Int,
    ): Page<Block>

    fun findPageByUserIdAndType(
        userId: Long,
        type: BlockType,
        page: Int,
        size: Int,
    ): Page<Block>

    fun countByUserId(userId: Long): Map<BlockType, Long>
}
