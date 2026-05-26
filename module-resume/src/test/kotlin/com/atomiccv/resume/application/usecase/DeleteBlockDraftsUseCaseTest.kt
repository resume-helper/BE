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

class DeleteBlockDraftsUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = DeleteBlockDraftsUseCase(blockDraftRepository)

    private fun draft(
        id: Long,
        userId: Long = 10L
    ) = BlockDraft(
        id = id,
        userId = userId,
        blockType = BlockType.CAREER,
        title = "제목 $id",
        contentJson = "{}",
        expiresAt = LocalDateTime.now().plusDays(14),
    )

    @Test
    fun `선택 삭제 시 deleteAllByIds가 호출된다`() {
        every { blockDraftRepository.findAllByIds(listOf(1L, 2L)) } returns listOf(draft(1L), draft(2L))
        every { blockDraftRepository.deleteAllByIds(any()) } just runs

        useCase.deleteAll(DeleteBlockDraftsCommand(draftIds = listOf(1L, 2L), userId = 10L))

        verify { blockDraftRepository.deleteAllByIds(listOf(1L, 2L)) }
    }

    @Test
    fun `ids가 비어 있으면 아무 동작도 하지 않는다`() {
        useCase.deleteAll(DeleteBlockDraftsCommand(draftIds = emptyList(), userId = 10L))

        verify(exactly = 0) { blockDraftRepository.findAllByIds(any()) }
    }

    @Test
    fun `타인의 드래프트가 포함되면 BLOCK_DRAFT_FORBIDDEN 예외가 발생한다`() {
        every { blockDraftRepository.findAllByIds(listOf(1L, 2L)) } returns listOf(draft(1L), draft(2L, userId = 99L))

        val ex =
            assertFailsWith<BusinessException> {
                useCase.deleteAll(DeleteBlockDraftsCommand(draftIds = listOf(1L, 2L), userId = 10L))
            }
        assertEquals(ErrorCode.BLOCK_DRAFT_FORBIDDEN, ex.errorCode)
    }
}
