package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.application.port.S3Port
import java.util.UUID

data class GenerateUploadUrlCommand(
    val userId: Long,
    val fileName: String,
)

data class UploadUrlResult(
    val presignedUrl: String,
    val s3Key: String,
)

class GenerateUploadUrlUseCase(
    private val s3Port: S3Port,
) {
    fun generate(command: GenerateUploadUrlCommand): UploadUrlResult {
        val key = buildKey(command)
        val url = s3Port.generateUploadPresignedUrl(key, UPLOAD_URL_EXPIRY_MINUTES)
        return UploadUrlResult(presignedUrl = url, s3Key = key)
    }

    private fun buildKey(command: GenerateUploadUrlCommand): String =
        "resumes/${command.userId}/${UUID.randomUUID()}/${command.fileName}"

    companion object {
        private const val UPLOAD_URL_EXPIRY_MINUTES = 10L
    }
}
