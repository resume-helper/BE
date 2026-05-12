package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeleteBlockUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = DeleteBlockUseCase(blockRepository)

    private val existingBlock =
        Block(id = 1L, userId = 10L, type = BlockType.SKILL, title = "Kotlin", contentJson = "{}")

    @Test
    fun `블록 삭제 시 deletedAt이 설정된 블록이 저장된다`() {
        every { blockRepository.findById(1L) } returns existingBlock
        every { blockRepository.save(any()) } returns existingBlock.copy(deletedAt = LocalDateTime.now())

        useCase.delete(blockId = 1L, userId = 10L)

        verify { blockRepository.save(match { it.deletedAt != null }) }
    }

    @Test
    fun `존재하지 않는 블록 삭제 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { blockRepository.findById(999L) } returns null

        val ex = assertFailsWith<BusinessException> { useCase.delete(blockId = 999L, userId = 10L) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `이미 삭제된 블록 삭제 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { blockRepository.findById(1L) } returns existingBlock.copy(deletedAt = LocalDateTime.now())

        val ex = assertFailsWith<BusinessException> { useCase.delete(blockId = 1L, userId = 10L) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 블록 삭제 시 FORBIDDEN 예외가 발생한다`() {
        every { blockRepository.findById(1L) } returns existingBlock

        val ex = assertFailsWith<BusinessException> { useCase.delete(blockId = 1L, userId = 99L) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
