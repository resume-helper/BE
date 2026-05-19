package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateResumeUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val useCase = UpdateResumeUseCase(resumeRepository)

    private fun resumeFixture(
        userId: Long = 1L,
        deleted: Boolean = false
    ) = Resume(
        id = 1L,
        userId = userId,
        title = "기존 제목",
        slug = "abc123",
        deletedAt = if (deleted) LocalDateTime.now() else null,
    )

    @Test
    fun `이력서 수정 시 title이 변경된다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture()
        every { resumeRepository.save(any()) } answers { firstArg() }
        justRun { resumeRepository.deleteBlocksByResumeId(any()) }
        every { resumeRepository.saveBlock(any()) } answers { firstArg() }

        val command =
            UpdateResumeCommand(
                resumeId = 1L,
                userId = 1L,
                title = "새 제목",
                blocks = emptyList(),
            )

        val result = useCase.update(command)

        assertEquals("새 제목", result.title)
    }

    @Test
    fun `수정 시 기존 blocks가 모두 삭제된 후 새 blocks가 저장된다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture()
        every { resumeRepository.save(any()) } answers { firstArg() }
        justRun { resumeRepository.deleteBlocksByResumeId(any()) }
        every { resumeRepository.saveBlock(any()) } answers { firstArg() }

        val command =
            UpdateResumeCommand(
                resumeId = 1L,
                userId = 1L,
                title = "새 제목",
                blocks =
                    listOf(
                        ResumeBlockInput(blockId = 5L, orderIndex = 0),
                    ),
            )

        useCase.update(command)

        verifyOrder {
            resumeRepository.deleteBlocksByResumeId(any())
            resumeRepository.saveBlock(any())
        }
    }

    @Test
    fun `존재하지 않는 이력서 수정 시 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findById(999L) } returns null

        val command =
            UpdateResumeCommand(
                resumeId = 999L,
                userId = 1L,
                title = "새 제목",
                blocks = emptyList(),
            )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `이미 삭제된 이력서 수정 시 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture(deleted = true)

        val command =
            UpdateResumeCommand(
                resumeId = 1L,
                userId = 1L,
                title = "새 제목",
                blocks = emptyList(),
            )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 이력서 수정 시 FORBIDDEN 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resumeFixture(userId = 1L)

        val command =
            UpdateResumeCommand(
                resumeId = 1L,
                userId = 99L,
                title = "새 제목",
                blocks = emptyList(),
            )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
