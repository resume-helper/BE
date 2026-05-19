package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class GetAllFeedbacksUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val feedbackRepository: FeedbackRepository = mockk()
    private val useCase = GetAllFeedbacksUseCase(resumeRepository, feedbackRepository)

    private val resume1 = Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "이력서1", isPublic = true)
    private val resume2 = Resume(id = 2L, userId = 10L, type = ResumeType.WEB, title = "이력서2", isPublic = true)
    private val deletedResume =
        Resume(id = 3L, userId = 10L, type = ResumeType.WEB, title = "삭제됨", isPublic = false)
            .copy(deletedAt = LocalDateTime.now())

    private val feedback1 =
        Feedback(id = 1L, resumeId = 1L, rating = 5, comment = null, reviewerIp = "1.1.1.1")
    private val feedback2 =
        Feedback(id = 2L, resumeId = 2L, rating = 3, comment = "보통", reviewerIp = "2.2.2.2")

    private val query = GetAllFeedbacksQuery(requestUserId = 10L, page = 1, size = 10)

    @Test
    fun `전체 이력서의 피드백 목록을 반환한다`() {
        every { resumeRepository.findAllByUserId(10L) } returns listOf(resume1, resume2)
        every { feedbackRepository.findAllByResumeIdIn(listOf(1L, 2L), 1, 10) } returns
            listOf(feedback1, feedback2)
        every { feedbackRepository.countByResumeIdIn(listOf(1L, 2L)) } returns 2L

        val result = useCase.getAll(query)

        assertEquals(2, result.feedbacks.size)
        assertEquals(2L, result.totalCount)
    }

    @Test
    fun `삭제된 이력서의 피드백은 포함하지 않는다`() {
        every { resumeRepository.findAllByUserId(10L) } returns listOf(resume1, deletedResume)
        every { feedbackRepository.findAllByResumeIdIn(listOf(1L), 1, 10) } returns listOf(feedback1)
        every { feedbackRepository.countByResumeIdIn(listOf(1L)) } returns 1L

        val result = useCase.getAll(query)

        assertEquals(1, result.feedbacks.size)
        assertEquals(1L, result.totalCount)
    }

    @Test
    fun `이력서가 없으면 빈 목록을 반환한다`() {
        every { resumeRepository.findAllByUserId(10L) } returns emptyList()
        every { feedbackRepository.findAllByResumeIdIn(emptyList(), 1, 10) } returns emptyList()
        every { feedbackRepository.countByResumeIdIn(emptyList()) } returns 0L

        val result = useCase.getAll(query)

        assertEquals(0, result.feedbacks.size)
        assertEquals(0L, result.totalCount)
    }
}
