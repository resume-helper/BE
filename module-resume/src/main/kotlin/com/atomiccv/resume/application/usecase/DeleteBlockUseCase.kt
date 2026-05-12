package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
class DeleteBlockUseCase(
    private val blockRepository: BlockRepository,
) {
    fun delete(
        blockId: Long,
        userId: Long
    ) {
        val block = findActiveBlock(blockId)
        if (!block.isOwnedBy(userId)) throw BusinessException(ErrorCode.FORBIDDEN)
        blockRepository.save(block.copy(deletedAt = LocalDateTime.now()))
    }

    private fun findActiveBlock(blockId: Long): Block {
        val block =
            blockRepository.findById(blockId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "블록을 찾을 수 없습니다.")
        if (block.isDeleted()) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "블록을 찾을 수 없습니다.")
        return block
    }
}
