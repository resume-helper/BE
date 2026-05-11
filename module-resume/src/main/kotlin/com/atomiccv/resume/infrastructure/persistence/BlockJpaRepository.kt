package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.jpa.repository.JpaRepository

interface BlockJpaRepository : JpaRepository<BlockJpaEntity, Long> {
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long): List<BlockJpaEntity>

    fun findAllByUserIdAndTypeAndDeletedAtIsNull(
        userId: Long,
        type: BlockType
    ): List<BlockJpaEntity>
}
