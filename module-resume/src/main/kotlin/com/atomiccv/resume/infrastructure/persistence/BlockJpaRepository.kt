package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.jpa.repository.JpaRepository

interface BlockJpaRepository : JpaRepository<BlockJpaEntity, Long> {
    fun findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId: Long): List<BlockJpaEntity>

    fun findAllByUserIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
        userId: Long,
        type: BlockType,
    ): List<BlockJpaEntity>
}
