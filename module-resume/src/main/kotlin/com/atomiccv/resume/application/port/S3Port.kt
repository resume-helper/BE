package com.atomiccv.resume.application.port

interface S3Port {
    fun generateUploadPresignedUrl(
        key: String,
        expiryMinutes: Long,
    ): String

    fun generateDownloadPresignedUrl(
        key: String,
        expiryMinutes: Long,
    ): String
}
