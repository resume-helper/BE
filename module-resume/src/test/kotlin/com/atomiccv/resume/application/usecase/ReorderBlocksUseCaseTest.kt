package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeBlock
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.ResumeBlockRepository
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

class ReorderBlocksUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val resumeBlockRepository: ResumeBlockRepository = mockk()
    private val useCase = ReorderBlocksUseCase(resumeRepository, resumeBlockRepository)

    private val resume =
        Resume(
            id = 1L,
            userId = 10L,
            type = ResumeType.WEB,
            title = "테스트 이력서",
            slug = "test-slug",
        )

    private val existingBlocks =
        listOf(
            ResumeBlock(id = 1L, resumeId = 1L, blockId = 100L, orderIndex = 0),
            ResumeBlock(id = 2L, resumeId = 1L, blockId = 200L, orderIndex = 1),
            ResumeBlock(id = 3L, resumeId = 1L, blockId = 300L, orderIndex = 2),
        )

    @Test
    fun `블록 순서 변경 시 deleteAll 후 새 순서로 saveAll이 호출된다`() {
        val command = ReorderBlocksCommand(resumeId = 1L, userId = 10L, blockIds = listOf(300L, 100L, 200L))
        every { resumeRepository.findById(1L) } returns resume
        every { resumeBlockRepository.findAllByResumeId(1L) } returns existingBlocks
        every { resumeBlockRepository.deleteAllByResumeId(1L) } returns Unit
        every { resumeBlockRepository.saveAll(any()) } returns emptyList()

        useCase.reorder(command)

        verify { resumeBlockRepository.deleteAllByResumeId(1L) }
        verify {
            resumeBlockRepository.saveAll(
                match { blocks ->
                    blocks[0].blockId == 300L &&
                        blocks[0].orderIndex == 0 &&
                        blocks[1].blockId == 100L &&
                        blocks[1].orderIndex == 1 &&
                        blocks[2].blockId == 200L &&
                        blocks[2].orderIndex == 2
                },
            )
        }
    }

    @Test
    fun `존재하지 않는 이력서 순서 변경 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findById(999L) } returns null

        val ex =
            assertFailsWith<BusinessException> {
                useCase.reorder(ReorderBlocksCommand(resumeId = 999L, userId = 10L, blockIds = listOf(100L)))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `삭제된 이력서 순서 변경 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume.copy(deletedAt = LocalDateTime.now())

        val ex =
            assertFailsWith<BusinessException> {
                useCase.reorder(ReorderBlocksCommand(resumeId = 1L, userId = 10L, blockIds = listOf(100L)))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 이력서 블록 순서 변경 시 FORBIDDEN 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume

        val ex =
            assertFailsWith<BusinessException> {
                useCase.reorder(ReorderBlocksCommand(resumeId = 1L, userId = 99L, blockIds = listOf(100L, 200L, 300L)))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `blockIds에 중복 ID가 있으면 VALIDATION_FAILED 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { resumeBlockRepository.findAllByResumeId(1L) } returns existingBlocks

        val ex =
            assertFailsWith<BusinessException> {
                useCase.reorder(ReorderBlocksCommand(resumeId = 1L, userId = 10L, blockIds = listOf(100L, 100L, 300L)))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `blockIds가 이력서의 블록 목록과 다르면 VALIDATION_FAILED 예외가 발생한다`() {
        every { resumeRepository.findById(1L) } returns resume
        every { resumeBlockRepository.findAllByResumeId(1L) } returns existingBlocks

        val ex =
            assertFailsWith<BusinessException> {
                useCase.reorder(ReorderBlocksCommand(resumeId = 1L, userId = 10L, blockIds = listOf(100L, 200L, 999L)))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }
}
