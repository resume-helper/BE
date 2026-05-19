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

class GetFeedbackListUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val feedbackRepository: FeedbackRepository = mockk()
    private val useCase = GetFeedbackListUseCase(resumeRepository, feedbackRepository)

    private val resume = Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "내 이력서", isPublic = true)

    private val feedback =
        Feedback(id = 1L, resumeId = 1L, rating = 5, comment = null, reviewerIp = "1.2.3.4", tags = emptyList())

    @Test
    fun `소유자가 피드백 목록을 조회하면 피드백 리스트와 총 개수를 반환한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { feedbackRepository.findAllByResumeId(1L, 1, 10) } returns listOf(feedback)
        every { feedbackRepository.countByResumeId(1L) } returns 1L

        val result = useCase.getList(GetFeedbackListQuery(resumeId = 1L, requestUserId = 10L, page = 1, size = 10))

        assertEquals(1, result.feedbacks.size)
        assertEquals(1L, result.totalCount)
    }

    @Test
    fun `소유자가 아닌 사용자가 조회 시 FORBIDDEN이 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume

        val ex =
            assertThrows<BusinessException> {
                useCase.getList(GetFeedbackListQuery(resumeId = 1L, requestUserId = 99L, page = 1, size = 10))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `존재하지 않는 이력서 조회 시 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findById(1L) } returns null

        val ex =
            assertThrows<BusinessException> {
                useCase.getList(GetFeedbackListQuery(resumeId = 1L, requestUserId = 10L, page = 1, size = 10))
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }
}
