package com.atomiccv.resume.infrastructure.s3

import com.atomiccv.resume.application.port.S3Port
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

class S3Adapter(
    private val presigner: S3Presigner,
    private val bucketName: String,
) : S3Port {
    override fun generateUploadPresignedUrl(
        key: String,
        expiryMinutes: Long,
    ): String {
        val duration = Duration.ofMinutes(expiryMinutes)
        val builder =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(duration)
                .putObjectRequest { it.bucket(bucketName).key(key) }
        val request = builder.build()
        return presigner
            .presignPutObject(request)
            .url()
            .toString()
    }

    override fun generateDownloadPresignedUrl(
        key: String,
        expiryMinutes: Long,
    ): String {
        val duration = Duration.ofMinutes(expiryMinutes)
        val builder =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(duration)
                .getObjectRequest { it.bucket(bucketName).key(key) }
        val request = builder.build()
        return presigner
            .presignGetObject(request)
            .url()
            .toString()
    }
}
