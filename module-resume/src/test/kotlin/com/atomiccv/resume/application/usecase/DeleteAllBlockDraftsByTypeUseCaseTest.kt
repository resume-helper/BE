package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test

class DeleteAllBlockDraftsByTypeUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = DeleteAllBlockDraftsByTypeUseCase(blockDraftRepository)

    @Test
    fun `타입별 전체 삭제 시 deleteAllByUserIdAndBlockType이 호출된다`() {
        every { blockDraftRepository.deleteAllByUserIdAndBlockType(10L, BlockType.CAREER) } just runs

        useCase.deleteAll(DeleteAllBlockDraftsByTypeCommand(userId = 10L, blockType = BlockType.CAREER))

        verify { blockDraftRepository.deleteAllByUserIdAndBlockType(10L, BlockType.CAREER) }
    }
}
