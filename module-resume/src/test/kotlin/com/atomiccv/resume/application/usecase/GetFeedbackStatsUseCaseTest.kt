package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetFeedbackStatsUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val feedbackRepository: FeedbackRepository = mockk()
    private val useCase = GetFeedbackStatsUseCase(resumeRepository, feedbackRepository)

    private val resume =
        Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "내 이력서", isPublic = true)

    private fun feedback(
        section: BlockType? = null,
        rating: Double? = null,
        tags: List<String> = emptyList(),
    ) = Feedback(resumeId = 1L, section = section, rating = rating, comment = null, reviewerIp = "1.1.1.1", tags = tags)

    @Test
    fun `전체·섹션별 카운트, 평균 별점, 태그 분포를 집계한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findAllByResumeId(1L) } returns
            listOf(
                feedback(rating = 4.0, tags = listOf("전체적으로 잘 읽혀요")),
                feedback(rating = 5.0, tags = listOf("전체적으로 잘 읽혀요", "완성도가 높아요")),
                feedback(section = BlockType.CAREER, rating = 3.0, tags = listOf("성과가 잘 드러나요")),
                feedback(section = BlockType.CAREER, rating = 4.0),
                feedback(section = BlockType.SKILL, tags = listOf("스택이 잘 정리됐어요")),
            )

        val stats = useCase.getStats(GetFeedbackStatsQuery(resumeId = 1L, requestUserId = 10L))

        assertEquals(5L, stats.totalCount)
        assertEquals(2L, stats.overall.count)
        assertEquals(4.5, stats.overall.averageRating)
        assertEquals(mapOf("전체적으로 잘 읽혀요" to 2L, "완성도가 높아요" to 1L), stats.overall.tagCounts)

        val career = stats.sections.first { it.section == BlockType.CAREER }
        assertEquals(2L, career.count)
        assertEquals(3.5, career.averageRating)
        assertEquals(mapOf("성과가 잘 드러나요" to 1L), career.tagCounts)

        val skill = stats.sections.first { it.section == BlockType.SKILL }
        assertEquals(1L, skill.count)
        assertNull(skill.averageRating)
    }

    @Test
    fun `피드백이 없으면 빈 통계를 반환한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findAllByResumeId(1L) } returns emptyList()

        val stats = useCase.getStats(GetFeedbackStatsQuery(resumeId = 1L, requestUserId = 10L))

        assertEquals(0L, stats.totalCount)
        assertEquals(0L, stats.overall.count)
        assertNull(stats.overall.averageRating)
        assertEquals(emptyList(), stats.sections)
    }

    @Test
    fun `소유자가 아니면 FORBIDDEN이 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume

        val ex =
            assertThrows<BusinessException> {
                useCase.getStats(GetFeedbackStatsQuery(resumeId = 1L, requestUserId = 99L))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `존재하지 않는 이력서면 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns null

        val ex =
            assertThrows<BusinessException> {
                useCase.getStats(GetFeedbackStatsQuery(resumeId = 1L, requestUserId = 10L))
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }
}
