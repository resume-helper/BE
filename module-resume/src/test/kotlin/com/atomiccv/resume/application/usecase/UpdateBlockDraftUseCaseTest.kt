package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UpdateBlockDraftUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = UpdateBlockDraftUseCase(blockDraftRepository)

    private val existingDraft =
        BlockDraft(
            id = 1L,
            userId = 10L,
            blockType = BlockType.CAREER,
            title = "원본 제목",
            contentJson = "{}",
            expiresAt = LocalDateTime.now().plusDays(14),
        )

    @Test
    fun `덮어쓰기 시 title·contentJson이 갱신되고 expiresAt이 연장된다`() {
        val command =
            UpdateBlockDraftCommand(
                draftId = 1L,
                userId = 10L,
                title = "수정 제목",
                contentJson = """{"company":"네이버"}""",
            )
        every { blockDraftRepository.findById(1L) } returns existingDraft
        every { blockDraftRepository.save(any()) } answers { firstArg() }

        val result = useCase.update(command)

        assertEquals("수정 제목", result.title)
        assertTrue(result.expiresAt.isAfter(LocalDateTime.now().plusDays(13)))
        verify { blockDraftRepository.save(match { it.title == "수정 제목" }) }
    }

    @Test
    fun `존재하지 않는 드래프트 수정 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        every { blockDraftRepository.findById(999L) } returns null
        val command =
            UpdateBlockDraftCommand(
                draftId = 999L,
                userId = 10L,
                title = "제목",
                contentJson = "{}",
            )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `만료된 드래프트 수정 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        val expiredDraft = existingDraft.copy(expiresAt = LocalDateTime.now().minusDays(1))
        every { blockDraftRepository.findById(1L) } returns expiredDraft
        val command =
            UpdateBlockDraftCommand(
                draftId = 1L,
                userId = 10L,
                title = "제목",
                contentJson = "{}",
            )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 드래프트 수정 시 BLOCK_DRAFT_FORBIDDEN 예외가 발생한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft
        val command =
            UpdateBlockDraftCommand(
                draftId = 1L,
                userId = 99L,
                title = "제목",
                contentJson = "{}",
            )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `title이 빈 문자열이면 BLOCK_DRAFT_TITLE_REQUIRED 예외가 발생한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft
        val command =
            UpdateBlockDraftCommand(
                draftId = 1L,
                userId = 10L,
                title = "   ",
                contentJson = "{}",
            )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_TITLE_REQUIRED, ex.errorCode)
    }
}
