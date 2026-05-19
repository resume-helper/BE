package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

data class Feedback(
    val id: Long = 0,
    val resumeId: Long,
    val rating: Int,
    val comment: String?,
    val reviewerIp: String,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
