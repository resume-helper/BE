package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals

class GetBlocksUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = GetBlocksUseCase(blockRepository)

    private val careerBlock = Block(id = 1L, userId = 10L, type = BlockType.CAREER, title = "경력1", contentJson = "{}")
    private val skillBlock = Block(id = 2L, userId = 10L, type = BlockType.SKILL, title = "기술1", contentJson = "{}")

    @Test
    fun `type 없이 조회하면 유저의 모든 활성 블록을 페이지로 반환한다`() {
        val pageable = PageRequest.of(0, 20)
        val pageResult = PageImpl(listOf(careerBlock, skillBlock), pageable, 2)
        every { blockRepository.findPageByUserId(10L, 0, 20) } returns pageResult

        val result = useCase.getBlocks(GetBlocksQuery(userId = 10L, type = null))

        assertEquals(2, result.content.size)
        verify { blockRepository.findPageByUserId(10L, 0, 20) }
    }

    @Test
    fun `type 필터를 주면 해당 type 블록만 페이지로 반환한다`() {
        val pageable = PageRequest.of(0, 20)
        val pageResult = PageImpl(listOf(careerBlock), pageable, 1)
        every { blockRepository.findPageByUserIdAndType(10L, BlockType.CAREER, 0, 20) } returns pageResult

        val result = useCase.getBlocks(GetBlocksQuery(userId = 10L, type = BlockType.CAREER))

        assertEquals(1, result.content.size)
        assertEquals(BlockType.CAREER, result.content[0].type)
        verify { blockRepository.findPageByUserIdAndType(10L, BlockType.CAREER, 0, 20) }
    }

    @Test
    fun `page와 size를 지정하면 해당 파라미터로 조회한다`() {
        val pageable = PageRequest.of(1, 5)
        val pageResult = PageImpl(listOf(careerBlock), pageable, 6)
        every { blockRepository.findPageByUserId(10L, 1, 5) } returns pageResult

        val result = useCase.getBlocks(GetBlocksQuery(userId = 10L, type = null, page = 2, size = 5))

        assertEquals(1, result.content.size)
        verify { blockRepository.findPageByUserId(10L, 1, 5) }
    }
}
