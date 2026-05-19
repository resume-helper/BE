package com.atomiccv.resume.infrastructure

import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteFeedbackUseCase
import com.atomiccv.resume.application.usecase.GetAllFeedbacksUseCase
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.GetFeedbackListUseCase
import com.atomiccv.resume.application.usecase.GetFeedbackUseCase
import com.atomiccv.resume.application.usecase.ReorderBlocksUseCase
import com.atomiccv.resume.application.usecase.SubmitFeedbackUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeBlockRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
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

    @Bean
    fun reorderBlocksUseCase(
        resumeRepository: ResumeRepository,
        resumeBlockRepository: ResumeBlockRepository,
    ): ReorderBlocksUseCase = ReorderBlocksUseCase(resumeRepository, resumeBlockRepository)

    @Bean
    fun submitFeedbackUseCase(
        resumeRepository: ResumeRepository,
        feedbackRepository: FeedbackRepository,
    ): SubmitFeedbackUseCase = SubmitFeedbackUseCase(resumeRepository, feedbackRepository)

    @Bean
    fun getFeedbackListUseCase(
        resumeRepository: ResumeRepository,
        feedbackRepository: FeedbackRepository,
    ): GetFeedbackListUseCase = GetFeedbackListUseCase(resumeRepository, feedbackRepository)

    @Bean
    fun getFeedbackUseCase(
        resumeRepository: ResumeRepository,
        feedbackRepository: FeedbackRepository,
    ): GetFeedbackUseCase = GetFeedbackUseCase(resumeRepository, feedbackRepository)

    @Bean
    fun deleteFeedbackUseCase(
        resumeRepository: ResumeRepository,
        feedbackRepository: FeedbackRepository,
    ): DeleteFeedbackUseCase = DeleteFeedbackUseCase(resumeRepository, feedbackRepository)

    @Bean
    fun getAllFeedbacksUseCase(
        resumeRepository: ResumeRepository,
        feedbackRepository: FeedbackRepository,
    ): GetAllFeedbacksUseCase = GetAllFeedbacksUseCase(resumeRepository, feedbackRepository)
}
