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
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SubmitFeedbackUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val feedbackRepository: FeedbackRepository = mockk()
    private val useCase = SubmitFeedbackUseCase(resumeRepository, feedbackRepository)

    private val publicResume =
        Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "내 이력서", isPublic = true)

    private val command =
        SubmitFeedbackCommand(
            resumeId = 1L,
            rating = 4,
            comment = "좋습니다",
            tags = listOf("성과중심"),
            reviewerIp = "1.2.3.4",
        )

    @Test
    fun `공개된 이력서에 피드백을 제출하면 저장된 피드백을 반환한다`() {
        val saved =
            Feedback(
                id = 1L,
                resumeId = 1L,
                rating = 4,
                comment = "좋습니다",
                reviewerIp = "1.2.3.4",
                tags = listOf("성과중심"),
            )
        every { resumeRepository.findById(1L) } returns publicResume
        every { feedbackRepository.save(any()) } returns saved

        val result = useCase.submit(command)

        assertEquals(1L, result.id)
        assertEquals(4, result.rating)
        verify { feedbackRepository.save(match { it.resumeId == 1L && it.rating == 4 && it.tags == listOf("성과중심") }) }
    }

    @Test
    fun `존재하지 않는 이력서에 피드백 제출 시 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns null

        val ex = assertThrows<BusinessException> { useCase.submit(command) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `비공개 이력서에 피드백 제출 시 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns publicResume.copy(isPublic = false)

        val ex = assertThrows<BusinessException> { useCase.submit(command) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `삭제된 이력서에 피드백 제출 시 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns
            publicResume.copy(deletedAt = java.time.LocalDateTime.now())

        val ex = assertThrows<BusinessException> { useCase.submit(command) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `이력서 소유자가 자신의 이력서에 피드백 제출 시 FORBIDDEN이 발생한다`() {
        every { resumeRepository.findById(1L) } returns publicResume

        val ex =
            assertThrows<BusinessException> {
                useCase.submit(command.copy(requestUserId = publicResume.userId))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
