package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.ViewSession
import java.time.LocalDateTime

/** 기간 내 조회수·평균 체류시간 집계 */
data class ViewAggregate(
    val viewCount: Long,
    /** 체류시간이 보고된 세션들의 평균(초). 없으면 null */
    val averageDurationSec: Double?,
)

/** 섹션별 평균 체류시간 */
data class SectionDwellAverage(
    val section: BlockType,
    val averageDwellSec: Double,
)

interface ViewSessionRepository {
    fun save(viewSession: ViewSession): ViewSession

    fun findById(id: Long): ViewSession?

    fun countByResumeId(resumeId: Long): Long

    fun countDistinctVisitorsByResumeId(resumeId: Long): Long

    fun aggregateByResumeIdBetween(
        resumeId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): ViewAggregate

    fun averageSectionDwellsByResumeId(resumeId: Long): List<SectionDwellAverage>
}
