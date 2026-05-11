package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

data class Block(
    val id: Long = 0,
    val userId: Long,
    val type: BlockType,
    val title: String,
    val contentJson: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
) {
    fun isDeleted(): Boolean = deletedAt != null

    fun isOwnedBy(ownerId: Long): Boolean = userId == ownerId
}
