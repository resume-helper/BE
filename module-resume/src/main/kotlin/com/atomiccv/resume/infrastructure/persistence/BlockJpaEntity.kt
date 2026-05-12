package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.infrastructure.persistence.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "blocks")
class BlockJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: BlockType,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(name = "content_json", nullable = false, columnDefinition = "JSON")
    val contentJson: String,
    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,
) : BaseJpaEntity() {
    fun toDomain() =
        Block(
            id = id,
            userId = userId,
            type = type,
            title = title,
            contentJson = contentJson,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )

    companion object {
        fun fromDomain(block: Block) =
            BlockJpaEntity(
                id = block.id,
                userId = block.userId,
                type = block.type,
                title = block.title,
                contentJson = block.contentJson,
                deletedAt = block.deletedAt,
            )
    }
}
