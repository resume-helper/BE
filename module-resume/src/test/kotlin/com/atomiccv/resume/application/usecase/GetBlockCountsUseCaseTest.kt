package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetBlockCountsUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = GetBlockCountsUseCase(blockRepository)

    @Test
    fun `일부 타입에만 블록이 있으면 나머지 타입은 0건으로 채워 전체 타입을 반환한다`() {
        every { blockRepository.countByUserId(10L) } returns
            mapOf(
                BlockType.CAREER to 3L,
                BlockType.PROJECT to 2L,
            )

        val result = useCase.getCounts(10L)

        assertEquals(BlockType.entries.size, result.counts.size)
        assertEquals(5L, result.totalCount)
        assertEquals(3L, result.counts.first { it.type == BlockType.CAREER }.count)
        assertEquals(2L, result.counts.first { it.type == BlockType.PROJECT }.count)
        assertEquals(0L, result.counts.first { it.type == BlockType.SKILL }.count)
        verify { blockRepository.countByUserId(10L) }
    }

    @Test
    fun `블록이 하나도 없으면 모든 타입이 0건이고 totalCount도 0이다`() {
        every { blockRepository.countByUserId(10L) } returns emptyMap()

        val result = useCase.getCounts(10L)

        assertEquals(0L, result.totalCount)
        assertEquals(BlockType.entries.size, result.counts.size)
        assertEquals(true, result.counts.all { it.count == 0L })
    }

    @Test
    fun `counts 순서는 BlockType enum 선언 순서를 따른다`() {
        every { blockRepository.countByUserId(10L) } returns emptyMap()

        val result = useCase.getCounts(10L)

        assertEquals(BlockType.entries, result.counts.map { it.type })
    }
}
