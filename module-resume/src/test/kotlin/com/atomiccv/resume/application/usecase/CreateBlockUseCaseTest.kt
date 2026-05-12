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

    @Test
    fun `블록 생성 시 userId·type·title·contentJson이 저장되고 반환된다`() {
        val command =
            CreateBlockCommand(
                userId = 1L,
                type = BlockType.CAREER,
                title = "카카오 백엔드 개발자",
                contentJson = """{"company":"카카오"}""",
            )
        val saved =
            Block(
                id = 10L,
                userId = 1L,
                type = BlockType.CAREER,
                title = "카카오 백엔드 개발자",
                contentJson = """{"company":"카카오"}"""
            )
        every { blockRepository.save(any()) } returns saved

        val result = useCase.create(command)

        assertEquals(10L, result.id)
        assertEquals(BlockType.CAREER, result.type)
        verify {
            blockRepository.save(
                match {
                    it.userId == 1L && it.type == BlockType.CAREER && it.title == "카카오 백엔드 개발자"
                },
            )
        }
    }
}
