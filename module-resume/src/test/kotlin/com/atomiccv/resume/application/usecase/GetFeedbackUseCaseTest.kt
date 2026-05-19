package com.atomiccv.resume.application.usecase

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

class GetFeedbackUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val feedbackRepository: FeedbackRepository = mockk()
    private val useCase = GetFeedbackUseCase(resumeRepository, feedbackRepository)

    private val resume = Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "내 이력서", isPublic = true)

    private val feedback =
        Feedback(id = 5L, resumeId = 1L, rating = 4, comment = "좋습니다", reviewerIp = "1.2.3.4", tags = listOf("성과중심"))

    @Test
    fun `소유자가 피드백을 단건 조회하면 피드백을 반환한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findById(5L) } returns feedback

        val result = useCase.get(GetFeedbackQuery(resumeId = 1L, feedbackId = 5L, requestUserId = 10L))

        assertEquals(5L, result.id)
        assertEquals(4, result.rating)
    }

    @Test
    fun `소유자가 아닌 사용자가 조회 시 FORBIDDEN이 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume

        val ex =
            assertThrows<BusinessException> {
                useCase.get(GetFeedbackQuery(resumeId = 1L, feedbackId = 5L, requestUserId = 99L))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `존재하지 않는 피드백 조회 시 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findById(5L) } returns null

        val ex =
            assertThrows<BusinessException> {
                useCase.get(GetFeedbackQuery(resumeId = 1L, feedbackId = 5L, requestUserId = 10L))
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `다른 이력서의 피드백 ID로 조회 시 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findById(5L) } returns feedback.copy(resumeId = 999L)

        val ex =
            assertThrows<BusinessException> {
                useCase.get(GetFeedbackQuery(resumeId = 1L, feedbackId = 5L, requestUserId = 10L))
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }
}
