package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeleteBlockDraftUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = DeleteBlockDraftUseCase(blockDraftRepository)

    private val existingDraft =
        BlockDraft(
            id = 1L,
            userId = 10L,
            blockType = BlockType.CAREER,
            title = "제목",
            contentJson = "{}",
            expiresAt = LocalDateTime.now().plusDays(14),
        )

    @Test
    fun `단건 삭제 시 deleteById가 호출된다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft
        every { blockDraftRepository.deleteById(1L) } just runs

        useCase.delete(DeleteBlockDraftCommand(draftId = 1L, userId = 10L))

        verify { blockDraftRepository.deleteById(1L) }
    }

    @Test
    fun `존재하지 않는 드래프트 삭제 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        every { blockDraftRepository.findById(999L) } returns null

        val ex =
            assertFailsWith<BusinessException> {
                useCase.delete(DeleteBlockDraftCommand(draftId = 999L, userId = 10L))
            }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 드래프트 삭제 시 BLOCK_DRAFT_FORBIDDEN 예외가 발생한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft

        val ex =
            assertFailsWith<BusinessException> {
                useCase.delete(DeleteBlockDraftCommand(draftId = 1L, userId = 99L))
            }
        assertEquals(ErrorCode.BLOCK_DRAFT_FORBIDDEN, ex.errorCode)
    }
}
