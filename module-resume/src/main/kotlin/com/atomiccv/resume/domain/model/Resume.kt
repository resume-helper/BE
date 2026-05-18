package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

data class Resume(
    val id: Long = 0,
    val userId: Long,
    val type: ResumeType? = null,
    val title: String,
    val slug: String? = null,
    val isPublic: Boolean = false,
    val pdfS3Key: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
) {
    fun isDeleted(): Boolean = deletedAt != null

    fun isOwnedBy(ownerId: Long): Boolean = userId == ownerId
}
