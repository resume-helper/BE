package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface BlockDraftJpaRepository : JpaRepository<BlockDraftJpaEntity, Long> {
    fun findAllByUserIdAndBlockTypeOrderByUpdatedAtDesc(
        userId: Long,
        blockType: BlockType,
    ): List<BlockDraftJpaEntity>

    @Modifying
    @Query("DELETE FROM BlockDraftJpaEntity b WHERE b.userId = :userId AND b.blockType = :blockType")
    fun deleteAllByUserIdAndBlockType(
        userId: Long,
        blockType: BlockType
    )

    @Modifying
    @Query("DELETE FROM BlockDraftJpaEntity b WHERE b.expiresAt < :threshold")
    fun deleteAllExpiredBefore(threshold: LocalDateTime): Int
}
