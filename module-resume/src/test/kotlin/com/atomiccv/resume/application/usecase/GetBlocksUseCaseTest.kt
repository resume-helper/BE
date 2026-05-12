package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetBlocksUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = GetBlocksUseCase(blockRepository)

    private val careerBlock = Block(id = 1L, userId = 10L, type = BlockType.CAREER, title = "경력1", contentJson = "{}")
    private val skillBlock = Block(id = 2L, userId = 10L, type = BlockType.SKILL, title = "기술1", contentJson = "{}")

    @Test
    fun `type 없이 조회하면 유저의 모든 활성 블록을 반환한다`() {
        every { blockRepository.findAllActiveByUserId(10L) } returns listOf(careerBlock, skillBlock)

        val result = useCase.getBlocks(GetBlocksQuery(userId = 10L, type = null))

        assertEquals(2, result.size)
        verify { blockRepository.findAllActiveByUserId(10L) }
    }

    @Test
    fun `type 필터를 주면 해당 type 블록만 반환한다`() {
        every { blockRepository.findAllActiveByUserIdAndType(10L, BlockType.CAREER) } returns listOf(careerBlock)

        val result = useCase.getBlocks(GetBlocksQuery(userId = 10L, type = BlockType.CAREER))

        assertEquals(1, result.size)
        assertEquals(BlockType.CAREER, result[0].type)
        verify { blockRepository.findAllActiveByUserIdAndType(10L, BlockType.CAREER) }
    }
}
