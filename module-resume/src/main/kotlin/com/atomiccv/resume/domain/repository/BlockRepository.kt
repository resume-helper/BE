package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType

interface BlockRepository {
    fun save(block: Block): Block

    fun findById(id: Long): Block?

    fun findAllActiveByUserId(userId: Long): List<Block>

    fun findAllActiveByUserIdAndType(
        userId: Long,
        type: BlockType
    ): List<Block>
}
