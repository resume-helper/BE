package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeBlock
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.ResumeRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class CreateResumeCommand(
    val userId: Long,
    val title: String,
    val type: ResumeType?,
    val blocks: List<ResumeBlockInput>,
)

data class ResumeBlockInput(
    val blockId: Long,
    val orderIndex: Int,
)

@Transactional
class CreateResumeUseCase(
    private val resumeRepository: ResumeRepository,
) {
    fun create(command: CreateResumeCommand): Resume {
        val resume = resumeRepository.save(buildResume(command))
        saveBlocks(resume.id, command.blocks)
        return resume
    }

    private fun buildResume(command: CreateResumeCommand) =
        Resume(
            userId = command.userId,
            title = command.title,
            type = command.type,
            slug = UUID.randomUUID().toString().replace("-", ""),
        )

    private fun saveBlocks(
        resumeId: Long,
        blocks: List<ResumeBlockInput>
    ) {
        blocks.forEach { input ->
            resumeRepository.saveBlock(
                ResumeBlock(resumeId = resumeId, blockId = input.blockId, orderIndex = input.orderIndex),
            )
        }
    }
}
