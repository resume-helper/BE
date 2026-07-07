package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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

    @Query(
        """
        SELECT b.type AS type,
               COUNT(b) AS count
        FROM BlockJpaEntity b
        WHERE b.userId = :userId
          AND b.deletedAt IS NULL
        GROUP BY b.type
    """,
    )
    fun countByUserIdGroupedByType(
        @Param("userId") userId: Long,
    ): List<BlockTypeCountProjection>
}

interface BlockTypeCountProjection {
    val type: BlockType
    val count: Long
}
