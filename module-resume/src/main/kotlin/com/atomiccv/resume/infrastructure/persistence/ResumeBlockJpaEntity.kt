package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.ResumeBlock
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "resume_blocks")
class ResumeBlockJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "resume_id", nullable = false)
    val resumeId: Long,
    @Column(name = "block_id", nullable = false)
    val blockId: Long,
    @Column(name = "order_index", nullable = false)
    val orderIndex: Int,
) {
    fun toDomain() =
        ResumeBlock(
            id = id,
            resumeId = resumeId,
            blockId = blockId,
            orderIndex = orderIndex,
        )

    companion object {
        fun fromDomain(resumeBlock: ResumeBlock) =
            ResumeBlockJpaEntity(
                id = resumeBlock.id,
                resumeId = resumeBlock.resumeId,
                blockId = resumeBlock.blockId,
                orderIndex = resumeBlock.orderIndex,
            )
    }
}
