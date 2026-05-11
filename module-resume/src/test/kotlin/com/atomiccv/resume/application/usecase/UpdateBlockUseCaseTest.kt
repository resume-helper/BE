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

class UpdateBlockUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = UpdateBlockUseCase(blockRepository)

    private val existingBlock =
        Block(
            id = 1L,
            userId = 10L,
            type = BlockType.CAREER,
            title = "구 제목",
            contentJson = "{}",
        )

    @Test
    fun `블록 수정 시 title과 contentJson이 업데이트된 블록이 저장된다`() {
        val command = UpdateBlockCommand(blockId = 1L, userId = 10L, title = "신 제목", contentJson = """{"new":true}""")
        val updated = existingBlock.copy(title = "신 제목", contentJson = """{"new":true}""")
        every { blockRepository.findById(1L) } returns existingBlock
        every { blockRepository.save(any()) } returns updated

        val result = useCase.update(command)

        assertEquals("신 제목", result.title)
        verify { blockRepository.save(match { it.title == "신 제목" && it.contentJson == """{"new":true}""" }) }
    }

    @Test
    fun `존재하지 않는 블록 수정 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { blockRepository.findById(999L) } returns null

        val ex =
            assertFailsWith<BusinessException> {
                useCase.update(UpdateBlockCommand(blockId = 999L, userId = 10L, title = "제목", contentJson = "{}"))
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `삭제된 블록 수정 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { blockRepository.findById(1L) } returns existingBlock.copy(deletedAt = LocalDateTime.now())

        val ex =
            assertFailsWith<BusinessException> {
                useCase.update(UpdateBlockCommand(blockId = 1L, userId = 10L, title = "제목", contentJson = "{}"))
            }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 블록 수정 시 FORBIDDEN 예외가 발생한다`() {
        every { blockRepository.findById(1L) } returns existingBlock

        val ex =
            assertFailsWith<BusinessException> {
                useCase.update(UpdateBlockCommand(blockId = 1L, userId = 99L, title = "제목", contentJson = "{}"))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
