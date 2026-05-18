package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.application.port.S3Port
import com.atomiccv.resume.domain.repository.ResumeDetail
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class GetResumeQuery(
    val resumeId: Long,
    val userId: Long,
)

class GetResumeUseCase(
    private val resumeRepository: ResumeRepository,
    private val s3Port: S3Port,
) {
    fun getDetail(query: GetResumeQuery): ResumeDetail {
        val detail =
            resumeRepository.findDetailById(query.resumeId)
                ?: throw BusinessException(ErrorCode.RESUME_NOT_FOUND)
        if (detail.resume.isDeleted()) throw BusinessException(ErrorCode.RESUME_NOT_FOUND)
        if (!detail.resume.isOwnedBy(query.userId)) throw BusinessException(ErrorCode.FORBIDDEN)
        return detail
    }

    fun getPresignedDownloadUrl(pdfS3Key: String): String =
        s3Port.generateDownloadPresignedUrl(pdfS3Key, DOWNLOAD_URL_EXPIRY_MINUTES)

    companion object {
        private const val DOWNLOAD_URL_EXPIRY_MINUTES = 15L
    }
}
