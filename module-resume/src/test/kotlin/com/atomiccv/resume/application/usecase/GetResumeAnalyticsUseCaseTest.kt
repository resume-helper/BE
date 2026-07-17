package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.SectionDwellAverage
import com.atomiccv.resume.domain.repository.ViewAggregate
import com.atomiccv.resume.domain.repository.ViewSessionRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class GetResumeAnalyticsUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val viewSessionRepository: ViewSessionRepository = mockk()
    private val useCase = GetResumeAnalyticsUseCase(resumeRepository, viewSessionRepository)

    private val resume =
        Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "이력서", isPublic = true)

    @Test
    fun `총열람·고유방문·최근7일 비교창·섹션별 평균 체류를 반환한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { viewSessionRepository.countByResumeId(1L) } returns 100L
        every { viewSessionRepository.countDistinctVisitorsByResumeId(1L) } returns 42L
        every { viewSessionRepository.aggregateByResumeIdBetween(1L, any(), any()) } returnsMany
            listOf(
                ViewAggregate(viewCount = 30L, averageDurationSec = 90.0),
                ViewAggregate(viewCount = 20L, averageDurationSec = 60.0),
            )
        every { viewSessionRepository.averageSectionDwellsByResumeId(1L) } returns
            listOf(SectionDwellAverage(section = BlockType.CAREER, averageDwellSec = 108.0))

        val result = useCase.getAnalytics(GetResumeAnalyticsQuery(resumeId = 1L, requestUserId = 10L))

        assertEquals(100L, result.totalViews)
        assertEquals(42L, result.uniqueVisitors)
        assertEquals(30L, result.last7Days.viewCount)
        assertEquals(20L, result.previous7Days.viewCount)
        assertEquals(90.0, result.last7Days.averageDurationSec)
        assertEquals(BlockType.CAREER, result.sectionDwells[0].section)
    }

    @Test
    fun `소유자가 아니면 FORBIDDEN이 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume

        val ex =
            assertThrows<BusinessException> {
                useCase.getAnalytics(GetResumeAnalyticsQuery(resumeId = 1L, requestUserId = 99L))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `존재하지 않는 이력서면 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns null

        val ex =
            assertThrows<BusinessException> {
                useCase.getAnalytics(GetResumeAnalyticsQuery(resumeId = 1L, requestUserId = 10L))
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }
}
