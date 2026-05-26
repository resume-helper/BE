package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

data class BlockDraft(
    val id: Long = 0,
    val userId: Long,
    val blockType: BlockType,
    val title: String,
    val contentJson: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isOwnedBy(ownerId: Long): Boolean = userId == ownerId

    companion object {
        const val EXPIRY_DAYS = 14L
    }
}
