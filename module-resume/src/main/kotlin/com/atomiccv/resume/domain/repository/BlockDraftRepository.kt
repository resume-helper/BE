package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import java.time.LocalDateTime

interface BlockDraftRepository {
    fun save(draft: BlockDraft): BlockDraft

    fun findById(id: Long): BlockDraft?

    fun findAllByUserIdAndBlockType(
        userId: Long,
        blockType: BlockType,
    ): List<BlockDraft>

    fun findAllByIds(ids: List<Long>): List<BlockDraft>

    fun deleteById(id: Long)

    fun deleteAllByIds(ids: List<Long>)

    fun deleteAllByUserIdAndBlockType(
        userId: Long,
        blockType: BlockType,
    )

    fun deleteAllExpiredBefore(threshold: LocalDateTime): Int
}
