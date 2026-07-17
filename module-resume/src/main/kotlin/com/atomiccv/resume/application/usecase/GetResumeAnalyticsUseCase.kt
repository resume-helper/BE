package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.SectionDwellAverage
import com.atomiccv.resume.domain.repository.ViewAggregate
import com.atomiccv.resume.domain.repository.ViewSessionRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import java.time.LocalDateTime

data class GetResumeAnalyticsQuery(
    val resumeId: Long,
    val requestUserId: Long,
)

data class ResumeAnalyticsResult(
    val totalViews: Long,
    val uniqueVisitors: Long,
    /** 최근 7일 (증감 표시용 비교창) */
    val last7Days: ViewAggregate,
    /** 직전 7일 (8~14일 전) */
    val previous7Days: ViewAggregate,
    val sectionDwells: List<SectionDwellAverage>,
)

/**
 * 이력서 소유자용 열람 분석 — 총열람·고유방문(IP 기준)·7일 비교창·섹션별 평균 체류시간.
 */
class GetResumeAnalyticsUseCase(
    private val resumeRepository: ResumeRepository,
    private val viewSessionRepository: ViewSessionRepository,
) {
    fun getAnalytics(query: GetResumeAnalyticsQuery): ResumeAnalyticsResult {
        verifyOwnedResume(query)
        val now = LocalDateTime.now()
        val sevenDaysAgo = now.minusDays(WINDOW_DAYS)
        val fourteenDaysAgo = now.minusDays(WINDOW_DAYS * 2)
        return ResumeAnalyticsResult(
            totalViews = viewSessionRepository.countByResumeId(query.resumeId),
            uniqueVisitors = viewSessionRepository.countDistinctVisitorsByResumeId(query.resumeId),
            last7Days = viewSessionRepository.aggregateByResumeIdBetween(query.resumeId, sevenDaysAgo, now),
            previous7Days =
                viewSessionRepository.aggregateByResumeIdBetween(query.resumeId, fourteenDaysAgo, sevenDaysAgo),
            sectionDwells = viewSessionRepository.averageSectionDwellsByResumeId(query.resumeId),
        )
    }

    private fun verifyOwnedResume(query: GetResumeAnalyticsQuery) {
        val resume = resumeRepository.findById(query.resumeId)
        if (resume == null || resume.isDeleted()) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        }
        if (!resume.isOwnedBy(query.requestUserId)) throw BusinessException(ErrorCode.FORBIDDEN)
    }

    companion object {
        private const val WINDOW_DAYS = 7L
    }
}
