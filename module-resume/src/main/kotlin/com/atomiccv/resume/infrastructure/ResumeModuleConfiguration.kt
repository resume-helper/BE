package com.atomiccv.resume.infrastructure

import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ResumeModuleConfiguration {
    @Bean
    fun createBlockUseCase(blockRepository: BlockRepository): CreateBlockUseCase = CreateBlockUseCase(blockRepository)

    @Bean
    fun updateBlockUseCase(blockRepository: BlockRepository): UpdateBlockUseCase = UpdateBlockUseCase(blockRepository)

    @Bean
    fun deleteBlockUseCase(blockRepository: BlockRepository): DeleteBlockUseCase = DeleteBlockUseCase(blockRepository)

    @Bean
    fun getBlocksUseCase(blockRepository: BlockRepository): GetBlocksUseCase = GetBlocksUseCase(blockRepository)
}
