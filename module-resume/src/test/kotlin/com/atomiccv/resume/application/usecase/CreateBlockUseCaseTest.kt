package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CreateBlockUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = CreateBlockUseCase(blockRepository)

    private val batchCommand =
        CreateBlocksCommand(
            userId = 1L,
            items =
                listOf(
                    BlockItemCommand(type = BlockType.BASIC_INFO, title = "기본 정보", contentJson = "{}"),
                    BlockItemCommand(
                        type = BlockType.CAREER,
                        title = "카카오 백엔드 개발자",
                        contentJson = """{"company":"카카오"}""",
                    ),
                ),
        )

    private val savedBlocks =
        listOf(
            Block(id = 1L, userId = 1L, type = BlockType.BASIC_INFO, title = "기본 정보", contentJson = "{}"),
            Block(
                id = 2L,
                userId = 1L,
                type = BlockType.CAREER,
                title = "카카오 백엔드 개발자",
                contentJson = """{"company":"카카오"}""",
            ),
        )

    @Test
    fun `블록 리스트 생성 시 items 수만큼 저장되고 반환된다`() {
        every { blockRepository.saveAll(any()) } returns savedBlocks

        val result = useCase.create(batchCommand)

        assertEquals(2, result.size)
        assertEquals(BlockType.BASIC_INFO, result[0].type)
        assertEquals(BlockType.CAREER, result[1].type)
        verify {
            blockRepository.saveAll(
                match { blocks ->
                    blocks.size == 2 &&
                        blocks[0].userId == 1L &&
                        blocks[0].type == BlockType.BASIC_INFO &&
                        blocks[1].userId == 1L &&
                        blocks[1].type == BlockType.CAREER
                },
            )
        }
    }

    @Test
    fun `빈 리스트로 생성 시 빈 리스트를 반환한다`() {
        val command = CreateBlocksCommand(userId = 1L, items = emptyList())
        every { blockRepository.saveAll(any()) } returns emptyList()

        val result = useCase.create(command)

        assertEquals(0, result.size)
        verify { blockRepository.saveAll(any()) }
    }
}
