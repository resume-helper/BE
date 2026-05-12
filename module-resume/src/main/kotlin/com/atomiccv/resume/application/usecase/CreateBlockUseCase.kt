package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.transaction.annotation.Transactional

data class CreateBlockCommand(
    val userId: Long,
    val type: BlockType,
    val title: String,
    val contentJson: String,
)

@Transactional
class CreateBlockUseCase(
    private val blockRepository: BlockRepository,
) {
    fun create(command: CreateBlockCommand): Block =
        blockRepository.save(
            Block(
                userId = command.userId,
                type = command.type,
                title = command.title,
                contentJson = command.contentJson,
            ),
        )
}
