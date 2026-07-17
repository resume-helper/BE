package com.atomiccv.resume.infrastructure

import com.atomiccv.resume.application.usecase.CreateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.CreateResumeUseCase
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteFeedbackUseCase
import com.atomiccv.resume.application.usecase.DeleteResumeUseCase
import com.atomiccv.resume.application.usecase.GetAllFeedbacksUseCase
import com.atomiccv.resume.application.usecase.GetBlockCountsUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.GetFeedbackListUseCase
import com.atomiccv.resume.application.usecase.GetFeedbackStatsUseCase
import com.atomiccv.resume.application.usecase.GetFeedbackUseCase
import com.atomiccv.resume.application.usecase.GetPublicResumeUseCase
import com.atomiccv.resume.application.usecase.GetResumeAnalyticsUseCase
import com.atomiccv.resume.application.usecase.GetResumeUseCase
import com.atomiccv.resume.application.usecase.GetResumesUseCase
import com.atomiccv.resume.application.usecase.RecordViewDurationUseCase
import com.atomiccv.resume.application.usecase.ReorderBlocksUseCase
import com.atomiccv.resume.application.usecase.StartViewSessionUseCase
import com.atomiccv.resume.application.usecase.SubmitFeedbackUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.application.usecase.UpdateResumeUseCase
import com.atomiccv.resume.application.usecase.UpdateResumeVisibilityUseCase
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeBlockRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.ViewSessionRepository
import com.atomiccv.shared.application.port.S3Port
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BlockUseCaseConfiguration {
    @Bean
    fun createBlockUseCase(blockRepository: BlockRepository): CreateBlockUseCase = CreateBlockUseCase(blockRepository)

    @Bean
    fun updateBlockUseCase(blockRepository: BlockRepository): UpdateBlockUseCase = UpdateBlockUseCase(blockRepository)

    @Bean
    fun deleteBlockUseCase(blockRepository: BlockRepository): DeleteBlockUseCase = DeleteBlockUseCase(blockRepository)

    @Bean
    fun getBlocksUseCase(blockRepository: BlockRepository): GetBlocksUseCase = GetBlocksUseCase(blockRepository)

    @Bean
    fun getBlockCountsUseCase(blockRepository: BlockRepository): GetBlockCountsUseCase =
        GetBlockCountsUseCase(blockRepository)
}

@Configuration
class BlockDraftUseCaseConfiguration {
    @Bean
    fun createBlockDraftUseCase(blockDraftRepository: BlockDraftRepository): CreateBlockDraftUseCase =
        CreateBlockDraftUseCase(blockDraftRepository)

    @Bean
    fun updateBlockDraftUseCase(blockDraftRepository: BlockDraftRepository): UpdateBlockDraftUseCase =
        UpdateBlockDraftUseCase(blockDraftRepository)

    @Bean
    fun getBlockDraftsUseCase(blockDraftRepository: BlockDraftRepository): GetBlockDraftsUseCase =
        GetBlockDraftsUseCase(blockDraftRepository)

    @Bean
    fun getBlockDraftUseCase(blockDraftRepository: BlockDraftRepository): GetBlockDraftUseCase =
        GetBlockDraftUseCase(blockDraftRepository)

    @Bean
    fun deleteBlockDraftUseCase(blockDraftRepository: BlockDraftRepository): DeleteBlockDraftUseCase =
        DeleteBlockDraftUseCase(blockDraftRepository)

    @Bean
    fun deleteBlockDraftsUseCase(blockDraftRepository: BlockDraftRepository): DeleteBlockDraftsUseCase =
        DeleteBlockDraftsUseCase(blockDraftRepository)

    @Bean
    fun deleteAllBlockDraftsByTypeUseCase(
        blockDraftRepository: BlockDraftRepository
    ): DeleteAllBlockDraftsByTypeUseCase = DeleteAllBlockDraftsByTypeUseCase(blockDraftRepository)
}

@Configuration
class ResumeUseCaseConfiguration {
    @Bean
    fun createResumeUseCase(resumeRepository: ResumeRepository): CreateResumeUseCase =
        CreateResumeUseCase(resumeRepository)

    @Bean
    fun updateResumeUseCase(resumeRepository: ResumeRepository): UpdateResumeUseCase =
        UpdateResumeUseCase(resumeRepository)

    @Bean
    fun deleteResumeUseCase(resumeRepository: ResumeRepository): DeleteResumeUseCase =
        DeleteResumeUseCase(resumeRepository)

    @Bean
    fun getResumeUseCase(
        resumeRepository: ResumeRepository,
        s3Port: S3Port,
    ): GetResumeUseCase = GetResumeUseCase(resumeRepository, s3Port)

    @Bean
    fun getResumesUseCase(resumeRepository: ResumeRepository): GetResumesUseCase = GetResumesUseCase(resumeRepository)

    @Bean
    fun getPublicResumeUseCase(resumeRepository: ResumeRepository): GetPublicResumeUseCase =
        GetPublicResumeUseCase(resumeRepository)

    @Bean
    fun updateResumeVisibilityUseCase(resumeRepository: ResumeRepository): UpdateResumeVisibilityUseCase =
        UpdateResumeVisibilityUseCase(resumeRepository)
}

@Configuration
class FeedbackUseCaseConfiguration {
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
    fun getFeedbackStatsUseCase(
        resumeRepository: ResumeRepository,
        feedbackRepository: FeedbackRepository,
    ): GetFeedbackStatsUseCase = GetFeedbackStatsUseCase(resumeRepository, feedbackRepository)

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

@Configuration
class AnalyticsUseCaseConfiguration {
    @Bean
    fun startViewSessionUseCase(
        resumeRepository: ResumeRepository,
        viewSessionRepository: ViewSessionRepository,
    ): StartViewSessionUseCase = StartViewSessionUseCase(resumeRepository, viewSessionRepository)

    @Bean
    fun recordViewDurationUseCase(
        resumeRepository: ResumeRepository,
        viewSessionRepository: ViewSessionRepository,
    ): RecordViewDurationUseCase = RecordViewDurationUseCase(resumeRepository, viewSessionRepository)

    @Bean
    fun getResumeAnalyticsUseCase(
        resumeRepository: ResumeRepository,
        viewSessionRepository: ViewSessionRepository,
    ): GetResumeAnalyticsUseCase = GetResumeAnalyticsUseCase(resumeRepository, viewSessionRepository)
}
