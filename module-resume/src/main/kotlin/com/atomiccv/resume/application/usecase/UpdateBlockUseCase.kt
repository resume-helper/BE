package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class UpdateBlockCommand(
    val blockId: Long,
    val userId: Long,
    val title: String,
    val contentJson: String,
)

@Transactional
class UpdateBlockUseCase(
    private val blockRepository: BlockRepository,
) {
    fun update(command: UpdateBlockCommand): Block {
        val block = findActiveBlock(command.blockId)
        if (!block.isOwnedBy(command.userId)) throw BusinessException(ErrorCode.FORBIDDEN)
        return blockRepository.save(block.copy(title = command.title, contentJson = command.contentJson))
    }

    private fun findActiveBlock(blockId: Long): Block {
        val block =
            blockRepository.findById(blockId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "블록을 찾을 수 없습니다.")
        if (block.isDeleted()) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "블록을 찾을 수 없습니다.")
        return block
    }
}
