package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

data class Feedback(
    val id: Long = 0,
    val resumeId: Long,
    /** 피드백 대상 섹션. null = 이력서 전체 피드백 */
    val section: BlockType? = null,
    /** 별점 1.0~5.0, 0.5 단위. 선택 항목 */
    val rating: Double?,
    val comment: String?,
    val reviewerIp: String,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
