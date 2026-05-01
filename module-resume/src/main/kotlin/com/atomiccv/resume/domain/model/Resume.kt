package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

data class Resume(
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val slug: String,
    val isPublished: Boolean = false,
    val currentVersionId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
)
