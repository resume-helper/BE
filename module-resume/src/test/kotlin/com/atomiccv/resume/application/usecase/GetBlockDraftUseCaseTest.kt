package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetBlockDraftUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = GetBlockDraftUseCase(blockDraftRepository)

    private val existingDraft =
        BlockDraft(
            id = 1L,
            userId = 10L,
            blockType = BlockType.CAREER,
            title = "제목",
            contentJson = """{"company":"카카오"}""",
            expiresAt = LocalDateTime.now().plusDays(14),
        )

    @Test
    fun `단건 조회 시 드래프트를 반환한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft

        val result = useCase.getDraft(GetBlockDraftQuery(draftId = 1L, userId = 10L))

        assertEquals(1L, result.id)
        assertEquals("제목", result.title)
    }

    @Test
    fun `존재하지 않는 드래프트 조회 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        every { blockDraftRepository.findById(999L) } returns null

        val ex =
            assertFailsWith<BusinessException> {
                useCase.getDraft(GetBlockDraftQuery(draftId = 999L, userId = 10L))
            }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `만료된 드래프트 조회 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        val expiredDraft = existingDraft.copy(expiresAt = LocalDateTime.now().minusDays(1))
        every { blockDraftRepository.findById(1L) } returns expiredDraft

        val ex =
            assertFailsWith<BusinessException> {
                useCase.getDraft(GetBlockDraftQuery(draftId = 1L, userId = 10L))
            }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 드래프트 조회 시 BLOCK_DRAFT_FORBIDDEN 예외가 발생한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft

        val ex =
            assertFailsWith<BusinessException> {
                useCase.getDraft(GetBlockDraftQuery(draftId = 1L, userId = 99L))
            }
        assertEquals(ErrorCode.BLOCK_DRAFT_FORBIDDEN, ex.errorCode)
    }
}
