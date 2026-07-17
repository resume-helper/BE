package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
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

class StartViewSessionUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val viewSessionRepository: ViewSessionRepository = mockk()
    private val useCase = StartViewSessionUseCase(resumeRepository, viewSessionRepository)

    private val publicResume =
        Resume(id = 1L, userId = 10L, type = ResumeType.WEB, title = "이력서", slug = "abc123", isPublic = true)

    @Test
    fun `공개 이력서 열람 시 세션을 생성하고 반환한다`() {
        every { resumeRepository.findBySlug("abc123") } returns publicResume
        every { viewSessionRepository.save(any()) } answers { firstArg<ViewSession>().copy(id = 7L) }

        val result = useCase.start(StartViewSessionCommand(slug = "abc123", visitorIp = "1.2.3.4"))

        assertEquals(7L, result.id)
        assertEquals(1L, result.resumeId)
        verify { viewSessionRepository.save(match { it.resumeId == 1L && it.visitorIp == "1.2.3.4" }) }
    }

    @Test
    fun `비공개 이력서면 RESUME_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findBySlug("abc123") } returns publicResume.copy(isPublic = false)

        val ex =
            assertThrows<BusinessException> {
                useCase.start(StartViewSessionCommand(slug = "abc123", visitorIp = "1.2.3.4"))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `존재하지 않는 slug면 RESUME_NOT_FOUND가 발생한다`() {
        every { resumeRepository.findBySlug("missing") } returns null

        val ex =
            assertThrows<BusinessException> {
                useCase.start(StartViewSessionCommand(slug = "missing", visitorIp = "1.2.3.4"))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }
}
