package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockDraft
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
@Table(name = "block_drafts")
class BlockDraftJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false)
    val blockType: BlockType,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(name = "content_json", columnDefinition = "JSON")
    val contentJson: String?,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
) : BaseJpaEntity() {
    fun toDomain() =
        BlockDraft(
            id = id,
            userId = userId,
            blockType = blockType,
            title = title,
            contentJson = contentJson ?: "{}",
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun fromDomain(draft: BlockDraft) =
            BlockDraftJpaEntity(
                id = draft.id,
                userId = draft.userId,
                blockType = draft.blockType,
                title = draft.title,
                contentJson = draft.contentJson,
                expiresAt = draft.expiresAt,
            )
    }
}
