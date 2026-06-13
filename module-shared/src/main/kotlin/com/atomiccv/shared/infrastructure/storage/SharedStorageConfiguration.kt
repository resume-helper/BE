package com.atomiccv.shared.infrastructure.storage

import com.atomiccv.shared.application.port.S3Port
import com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class SharedStorageConfiguration {
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
    fun generateUploadUrlUseCase(s3Port: S3Port): GenerateUploadUrlUseCase = GenerateUploadUrlUseCase(s3Port)
}
