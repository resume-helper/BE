package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.ResumeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class CreateResumeUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val useCase = CreateResumeUseCase(resumeRepository)

    @Test
    fun `WEB 타입 이력서 생성 시 slug가 자동 생성된다`() {
        every { resumeRepository.save(any()) } answers { firstArg<Resume>().copy(id = 1L) }
        every { resumeRepository.saveBlock(any()) } answers { firstArg() }

        val command =
            CreateResumeCommand(
                userId = 1L,
                title = "테스트 이력서",
                type = ResumeType.WEB,
                blocks = emptyList(),
            )

        val result = useCase.create(command)

        assertNotNull(result.slug)
        assertFalse(result.slug!!.isBlank())
    }

    @Test
    fun `PDF 타입 이력서 생성 시 slug가 생성되지 않고 pdfS3Key가 저장된다`() {
        every { resumeRepository.save(any()) } answers { firstArg<Resume>().copy(id = 1L) }

        val command =
            CreateResumeCommand(
                userId = 1L,
                title = "PDF 이력서",
                type = ResumeType.PDF,
                pdfS3Key = "resumes/1/uuid/resume.pdf",
                blocks = emptyList(),
            )

        useCase.create(command)

        verify {
            resumeRepository.save(
                match { it.slug == null && it.pdfS3Key == "resumes/1/uuid/resume.pdf" },
            )
        }
    }

    @Test
    fun `이력서 생성 시 블록 목록이 순서대로 저장된다`() {
        every { resumeRepository.save(any()) } answers { firstArg<Resume>().copy(id = 1L) }
        every { resumeRepository.saveBlock(any()) } answers { firstArg() }

        val command =
            CreateResumeCommand(
                userId = 1L,
                title = "블록 포함 이력서",
                type = null,
                blocks =
                    listOf(
                        ResumeBlockInput(blockId = 10L, orderIndex = 0),
                        ResumeBlockInput(blockId = 20L, orderIndex = 1),
                    ),
            )

        useCase.create(command)

        verify(exactly = 2) { resumeRepository.saveBlock(any()) }
        verify {
            resumeRepository.saveBlock(
                match { it.blockId == 10L && it.orderIndex == 0 },
            )
        }
        verify {
            resumeRepository.saveBlock(
                match { it.blockId == 20L && it.orderIndex == 1 },
            )
        }
    }

    @Test
    fun `이력서 생성 시 userId와 title이 올바르게 저장된다`() {
        every { resumeRepository.save(any()) } answers { firstArg<Resume>().copy(id = 1L) }
        every { resumeRepository.saveBlock(any()) } answers { firstArg() }

        val command =
            CreateResumeCommand(
                userId = 42L,
                title = "내 이력서",
                type = null,
                blocks = emptyList(),
            )

        val result = useCase.create(command)

        assertEquals(42L, result.userId)
        assertEquals("내 이력서", result.title)
    }
}
