package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class DeleteFeedbackUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val feedbackRepository: FeedbackRepository = mockk()
    private val useCase = DeleteFeedbackUseCase(resumeRepository, feedbackRepository)

    private val resume = Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "내 이력서", isPublic = true)

    private val feedback =
        Feedback(id = 5L, resumeId = 1L, rating = 4, comment = null, reviewerIp = "1.2.3.4")

    @Test
    fun `소유자가 피드백을 삭제하면 deleteById가 호출된다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findById(5L) } returns feedback
        every { feedbackRepository.deleteById(5L) } just runs

        useCase.delete(resumeId = 1L, feedbackId = 5L, requestUserId = 10L)

        verify { feedbackRepository.deleteById(5L) }
    }

    @Test
    fun `소유자가 아닌 사용자가 삭제 시 FORBIDDEN이 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume

        val ex =
            assertThrows<BusinessException> {
                useCase.delete(resumeId = 1L, feedbackId = 5L, requestUserId = 99L)
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `존재하지 않는 피드백 삭제 시 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findById(5L) } returns null

        val ex =
            assertThrows<BusinessException> {
                useCase.delete(resumeId = 1L, feedbackId = 5L, requestUserId = 10L)
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `다른 이력서의 피드백 삭제 시 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findById(5L) } returns feedback.copy(resumeId = 999L)

        val ex =
            assertThrows<BusinessException> {
                useCase.delete(resumeId = 1L, feedbackId = 5L, requestUserId = 10L)
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }
}
