package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateResumeVisibilityUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val useCase = UpdateResumeVisibilityUseCase(resumeRepository)

    private fun resumeFixture(
        userId: Long = 1L,
        deleted: Boolean = false,
        isPublic: Boolean = false,
    ) = Resume(
        id = 1L,
        userId = userId,
        title = "이력서",
        slug = "abc123",
        isPublic = isPublic,
        deletedAt = if (deleted) LocalDateTime.now() else null,
    )

    @Test
    fun `isPublic을 true로 변경하면 저장된다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture(isPublic = false)
        every { resumeRepository.save(any()) } answers { firstArg() }

        val result =
            useCase.update(
                UpdateResumeVisibilityCommand(resumeId = 1L, userId = 1L, isPublic = true),
            )

        assertTrue(result.isPublic)
    }

    @Test
    fun `isPublic을 false로 변경하면 저장된다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture(isPublic = true)
        every { resumeRepository.save(any()) } answers { firstArg() }

        val result =
            useCase.update(
                UpdateResumeVisibilityCommand(resumeId = 1L, userId = 1L, isPublic = false),
            )

        assertFalse(result.isPublic)
    }

    @Test
    fun `존재하지 않는 이력서의 공개 설정 시 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findById(999L) } returns null

        val ex =
            assertFailsWith<BusinessException> {
                useCase.update(UpdateResumeVisibilityCommand(resumeId = 999L, userId = 1L, isPublic = true))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `이미 삭제된 이력서의 공개 설정 시 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture(deleted = true)

        val ex =
            assertFailsWith<BusinessException> {
                useCase.update(UpdateResumeVisibilityCommand(resumeId = 1L, userId = 1L, isPublic = true))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 이력서 공개 설정 시 FORBIDDEN 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture(userId = 1L)

        val ex =
            assertFailsWith<BusinessException> {
                useCase.update(UpdateResumeVisibilityCommand(resumeId = 1L, userId = 99L, isPublic = true))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
