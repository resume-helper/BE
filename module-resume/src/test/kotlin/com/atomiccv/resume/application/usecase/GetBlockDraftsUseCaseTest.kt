package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetBlockDraftsUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = GetBlockDraftsUseCase(blockDraftRepository)

    private fun draft(
        id: Long,
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(14)
    ) = BlockDraft(
        id = id,
        userId = 10L,
        blockType = BlockType.CAREER,
        title = "제목 $id",
        contentJson = "{}",
        expiresAt = expiresAt,
    )

    @Test
    fun `목록 조회 시 만료되지 않은 드래프트만 반환된다`() {
        val active = draft(1L)
        val expired = draft(2L, expiresAt = LocalDateTime.now().minusDays(1))
        every { blockDraftRepository.findAllByUserIdAndBlockType(10L, BlockType.CAREER) } returns
            listOf(active, expired)

        val result =
            useCase.getDrafts(
                GetBlockDraftsQuery(userId = 10L, blockType = BlockType.CAREER, currentDraftId = null)
            )

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `currentDraftId와 일치하는 항목은 isActive가 true다`() {
        every { blockDraftRepository.findAllByUserIdAndBlockType(10L, BlockType.CAREER) } returns
            listOf(draft(1L), draft(2L))

        val result =
            useCase.getDrafts(
                GetBlockDraftsQuery(userId = 10L, blockType = BlockType.CAREER, currentDraftId = 1L)
            )

        assertTrue(result.find { it.id == 1L }!!.isActive)
        assertFalse(result.find { it.id == 2L }!!.isActive)
    }

    @Test
    fun `currentDraftId가 null이면 모든 항목의 isActive가 false다`() {
        every { blockDraftRepository.findAllByUserIdAndBlockType(10L, BlockType.CAREER) } returns listOf(draft(1L))

        val result =
            useCase.getDrafts(
                GetBlockDraftsQuery(userId = 10L, blockType = BlockType.CAREER, currentDraftId = null)
            )

        assertFalse(result[0].isActive)
    }
}
