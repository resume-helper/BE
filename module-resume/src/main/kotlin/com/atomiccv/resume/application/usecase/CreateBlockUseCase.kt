package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.transaction.annotation.Transactional

data class BlockItemCommand(
    val type: BlockType,
    val title: String,
    val contentJson: String,
)

data class CreateBlocksCommand(
    val userId: Long,
    val items: List<BlockItemCommand>,
)

@Transactional
class CreateBlockUseCase(
    private val blockRepository: BlockRepository,
) {
    fun create(command: CreateBlocksCommand): List<Block> =
        blockRepository.saveAll(
            command.items.map { item ->
                Block(
                    userId = command.userId,
                    type = item.type,
                    title = item.title,
                    contentJson = item.contentJson,
                )
            },
        )
}
