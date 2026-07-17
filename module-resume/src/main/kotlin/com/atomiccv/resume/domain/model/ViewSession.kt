package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

/**
 * 웹 이력서 열람 세션 (익명 열람자 1회 방문 = 세션 1개).
 * 생성 시점에 조회수 1로 집계되고, 이탈 시 체류시간이 보고된다.
 */
data class ViewSession(
    val id: Long = 0,
    val resumeId: Long,
    val visitorIp: String,
    /** 전체 체류시간(초). null = 아직 보고되지 않음 */
    val totalDurationSec: Int? = null,
    val sectionDwells: List<SectionDwell> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

/** 섹션(BlockType 카테고리)별 체류시간 */
data class SectionDwell(
    val section: BlockType,
    val dwellSeconds: Int,
)
