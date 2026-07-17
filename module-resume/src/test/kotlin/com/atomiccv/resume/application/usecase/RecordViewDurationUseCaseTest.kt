package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.model.SectionDwell
import com.atomiccv.resume.domain.model.ViewSession
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.ViewSessionRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RecordViewDurationUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val viewSessionRepository: ViewSessionRepository = mockk()
    private val useCase = RecordViewDurationUseCase(resumeRepository, viewSessionRepository)

    private val publicResume =
        Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "이력서", slug = "abc123", isPublic = true)
    private val session = ViewSession(id = 7L, resumeId = 1L, visitorIp = "1.2.3.4")

    private val command =
        RecordViewDurationCommand(
            slug = "abc123",
            sessionId = 7L,
            totalDurationSec = 120,
            sectionDwells = listOf(SectionDwell(section = BlockType.CAREER, dwellSeconds = 45)),
        )

    @Test
    fun `체류시간을 보고하면 세션이 갱신된다`() {
        every { resumeRepository.findBySlug("abc123") } returns publicResume
        every { viewSessionRepository.findById(7L) } returns session
        every { viewSessionRepository.save(any()) } answers { firstArg() }

        val result = useCase.record(command)

        assertEquals(120, result.totalDurationSec)
        assertEquals(1, result.sectionDwells.size)
        verify {
            viewSessionRepository.save(
                match { it.id == 7L && it.totalDurationSec == 120 && it.sectionDwells.size == 1 },
            )
        }
    }

    @Test
    fun `세션이 없으면 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findBySlug("abc123") } returns publicResume
        every { viewSessionRepository.findById(7L) } returns null

        val ex = assertThrows<BusinessException> { useCase.record(command) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `세션이 다른 이력서 소속이면 RESOURCE_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findBySlug("abc123") } returns publicResume
        every { viewSessionRepository.findById(7L) } returns session.copy(resumeId = 999L)

        val ex = assertThrows<BusinessException> { useCase.record(command) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `음수 체류시간은 VALIDATION_FAILED가 발생한다`() {
        every { resumeRepository.findBySlug("abc123") } returns publicResume
        every { viewSessionRepository.findById(7L) } returns session

        val ex = assertThrows<BusinessException> { useCase.record(command.copy(totalDurationSec = -1)) }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `음수 섹션 체류시간은 VALIDATION_FAILED가 발생한다`() {
        every { resumeRepository.findBySlug("abc123") } returns publicResume
        every { viewSessionRepository.findById(7L) } returns session

        val ex =
            assertThrows<BusinessException> {
                useCase.record(
                    command.copy(sectionDwells = listOf(SectionDwell(BlockType.CAREER, -5))),
                )
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }
}
