package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.ResumeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ResumeJpaRepository : JpaRepository<ResumeJpaEntity, Long> {
    fun findAllByUserIdAndDeletedAtIsNull(
        userId: Long,
        pageable: Pageable
    ): Page<ResumeJpaEntity>

    fun findAllByUserIdAndTypeAndDeletedAtIsNull(
        userId: Long,
        type: ResumeType,
        pageable: Pageable,
    ): Page<ResumeJpaEntity>

    fun findAllByUserIdAndTitleContainingAndDeletedAtIsNull(
        userId: Long,
        title: String,
        pageable: Pageable,
    ): Page<ResumeJpaEntity>

    fun findAllByUserIdAndTypeAndTitleContainingAndDeletedAtIsNull(
        userId: Long,
        type: ResumeType,
        title: String,
        pageable: Pageable,
    ): Page<ResumeJpaEntity>

    fun findBySlugAndDeletedAtIsNull(slug: String): ResumeJpaEntity?

    @Query(
        """
        SELECT rb.blockId AS blockId,
               rb.orderIndex AS orderIndex,
               b.title AS title,
               b.type AS type,
               b.contentJson AS contentJson
        FROM ResumeBlockJpaEntity rb
        JOIN BlockJpaEntity b ON b.id = rb.blockId
        WHERE rb.resumeId = :resumeId
          AND b.deletedAt IS NULL
        ORDER BY rb.orderIndex ASC
    """,
    )
    fun findBlockDetailsByResumeId(
        @Param("resumeId") resumeId: Long,
    ): List<ResumeBlockDetailProjection>
}

interface ResumeBlockDetailProjection {
    val blockId: Long
    val orderIndex: Int
    val title: String
    val type: BlockType
    val contentJson: String
}
