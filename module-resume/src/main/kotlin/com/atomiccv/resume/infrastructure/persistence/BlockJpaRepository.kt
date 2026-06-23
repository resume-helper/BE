package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface BlockJpaRepository : JpaRepository<BlockJpaEntity, Long> {
    fun findAllByUserIdAndDeletedAtIsNull(
        userId: Long,
        pageable: Pageable,
    ): Page<BlockJpaEntity>

    fun findAllByUserIdAndTypeAndDeletedAtIsNull(
        userId: Long,
        type: BlockType,
        pageable: Pageable,
    ): Page<BlockJpaEntity>
}
