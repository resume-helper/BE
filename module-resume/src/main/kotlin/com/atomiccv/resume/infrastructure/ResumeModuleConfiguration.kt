package com.atomiccv.resume.infrastructure

import com.atomiccv.resume.application.port.S3Port
import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.CreateResumeUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteResumeUseCase
import com.atomiccv.resume.application.usecase.GenerateUploadUrlUseCase
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.GetResumeUseCase
import com.atomiccv.resume.application.usecase.GetResumesUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.application.usecase.UpdateResumeUseCase
import com.atomiccv.resume.application.usecase.UpdateResumeVisibilityUseCase
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.infrastructure.s3.S3Adapter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

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

@Configuration
class ResumeUseCaseConfiguration {
    @Bean
    fun s3Presigner(
        @Value("\${cloud.aws.region.static}") region: String,
    ): S3Presigner = S3Presigner.builder().region(Region.of(region)).build()

    @Bean
    fun s3Port(
        presigner: S3Presigner,
        @Value("\${resume.s3.bucket-name}") bucketName: String,
    ): S3Port = S3Adapter(presigner, bucketName)

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
    fun updateResumeVisibilityUseCase(resumeRepository: ResumeRepository): UpdateResumeVisibilityUseCase =
        UpdateResumeVisibilityUseCase(resumeRepository)

    @Bean
    fun generateUploadUrlUseCase(s3Port: S3Port): GenerateUploadUrlUseCase = GenerateUploadUrlUseCase(s3Port)
}
