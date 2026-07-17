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
            rating = 4.0,
            comment = "좋습니다",
            tags = listOf("전체적으로 잘 읽혀요"),
            reviewerIp = "1.2.3.4",
        )

    @Test
    fun `공개된 이력서에 전체 피드백을 제출하면 저장된 피드백을 반환한다`() {
        val saved =
            Feedback(
                id = 1L,
                resumeId = 1L,
                rating = 4.0,
                comment = "좋습니다",
                reviewerIp = "1.2.3.4",
                tags = listOf("전체적으로 잘 읽혀요"),
            )
        every { resumeRepository.findById(1L) } returns publicResume
        every { feedbackRepository.save(any()) } returns saved

        val result = useCase.submit(command)

        assertEquals(1L, result.id)
        assertEquals(4.0, result.rating)
        verify {
            feedbackRepository.save(
                match { it.resumeId == 1L && it.rating == 4.0 && it.tags == listOf("전체적으로 잘 읽혀요") },
            )
        }
    }

    @Test
    fun `섹션 피드백을 0_5 단위 별점과 섹션 태그로 제출할 수 있다`() {
        every { resumeRepository.findById(1L) } returns publicResume
        every { feedbackRepository.save(any()) } answers { firstArg() }

        val result =
            useCase.submit(
                command.copy(
                    section = BlockType.CAREER,
                    rating = 4.5,
                    comment = null,
                    tags = listOf("성과가 잘 드러나요"),
                ),
            )

        assertEquals(BlockType.CAREER, result.section)
        assertEquals(4.5, result.rating)
    }

    @Test
    fun `별점 없이 태그만으로도 제출할 수 있다`() {
        every { resumeRepository.findById(1L) } returns publicResume
        every { feedbackRepository.save(any()) } answers { firstArg() }

        val result = useCase.submit(command.copy(rating = null, comment = null))

        assertEquals(null, result.rating)
    }

    @Test
    fun `별점·태그·텍스트가 모두 비어 있으면 VALIDATION_FAILED가 발생한다`() {
        every { resumeRepository.findById(1L) } returns publicResume

        val ex =
            assertThrows<BusinessException> {
                useCase.submit(command.copy(rating = null, comment = null, tags = emptyList()))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `0_5 단위가 아닌 별점은 VALIDATION_FAILED가 발생한다`() {
        every { resumeRepository.findById(1L) } returns publicResume

        val ex = assertThrows<BusinessException> { useCase.submit(command.copy(rating = 4.3)) }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `범위를 벗어난 별점은 VALIDATION_FAILED가 발생한다`() {
        every { resumeRepository.findById(1L) } returns publicResume

        val ex = assertThrows<BusinessException> { useCase.submit(command.copy(rating = 5.5)) }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `섹션 피드백에 텍스트를 넣으면 VALIDATION_FAILED가 발생한다`() {
        every { resumeRepository.findById(1L) } returns publicResume

        val ex =
            assertThrows<BusinessException> {
                useCase.submit(
                    command.copy(section = BlockType.CAREER, comment = "텍스트", tags = emptyList()),
                )
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `카탈로그에 없는 섹션(CUSTOM)에는 피드백을 남길 수 없다`() {
        every { resumeRepository.findById(1L) } returns publicResume

        val ex =
            assertThrows<BusinessException> {
                useCase.submit(command.copy(section = BlockType.CUSTOM, comment = null, tags = emptyList()))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `허용되지 않은 태그는 VALIDATION_FAILED가 발생한다`() {
        every { resumeRepository.findById(1L) } returns publicResume

        val ex =
            assertThrows<BusinessException> {
                useCase.submit(command.copy(tags = listOf("존재하지 않는 태그")))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `섹션 태그를 전체 피드백에 쓰면 VALIDATION_FAILED가 발생한다`() {
        every { resumeRepository.findById(1L) } returns publicResume

        val ex =
            assertThrows<BusinessException> {
                useCase.submit(command.copy(section = null, tags = listOf("성과가 잘 드러나요")))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
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
