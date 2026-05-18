package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeleteResumeUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val useCase = DeleteResumeUseCase(resumeRepository)

    private fun resumeFixture(
        userId: Long = 1L,
        deleted: Boolean = false
    ) = Resume(
        id = 1L,
        userId = userId,
        title = "삭제 대상 이력서",
        slug = "abc123",
        deletedAt = if (deleted) LocalDateTime.now() else null,
    )

    @Test
    fun `이력서 삭제 시 deletedAt이 설정된다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture()
        every { resumeRepository.save(any()) } answers { firstArg() }

        useCase.delete(resumeId = 1L, userId = 1L)

        verify { resumeRepository.save(match { it.deletedAt != null }) }
    }

    @Test
    fun `존재하지 않는 이력서 삭제 시 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findById(999L) } returns null

        val ex = assertFailsWith<BusinessException> { useCase.delete(resumeId = 999L, userId = 1L) }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `이미 삭제된 이력서 삭제 시 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture(deleted = true)

        val ex = assertFailsWith<BusinessException> { useCase.delete(resumeId = 1L, userId = 1L) }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 이력서 삭제 시 FORBIDDEN 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture(userId = 1L)

        val ex = assertFailsWith<BusinessException> { useCase.delete(resumeId = 1L, userId = 99L) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
