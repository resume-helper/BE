package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.application.port.S3Port
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeDetail
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class GetResumeUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val s3Port: S3Port = mockk()
    private val useCase = GetResumeUseCase(resumeRepository, s3Port)

    private fun resumeFixture(
        userId: Long = 1L,
        deleted: Boolean = false,
        pdfS3Key: String? = null,
    ) = Resume(
        id = 1L,
        userId = userId,
        title = "제목",
        slug = "abc",
        pdfS3Key = pdfS3Key,
        deletedAt = if (deleted) LocalDateTime.now() else null,
    )

    private fun detailFixture(
        userId: Long = 1L,
        deleted: Boolean = false,
        pdfS3Key: String? = null,
    ) = ResumeDetail(
        resume = resumeFixture(userId = userId, deleted = deleted, pdfS3Key = pdfS3Key),
        blocks = emptyList(),
    )

    @Test
    fun `이력서 상세 조회 시 blocks 포함 데이터를 반환한다`() {
        every { resumeRepository.findDetailById(1L) } returns detailFixture()

        val result = useCase.getDetail(GetResumeQuery(resumeId = 1L, userId = 1L))

        assertNotNull(result)
        assertEquals(1L, result.resume.id)
        assertEquals(emptyList(), result.blocks)
    }

    @Test
    fun `PDF 이력서 조회 시 presigned URL이 생성된다`() {
        val pdfKey = "resumes/1/uuid/file.pdf"
        every { resumeRepository.findDetailById(1L) } returns detailFixture(pdfS3Key = pdfKey)
        every { s3Port.generateDownloadPresignedUrl(pdfKey, any()) } returns "https://s3.example.com/download"

        val url = useCase.getPresignedDownloadUrl(pdfKey)

        assertEquals("https://s3.example.com/download", url)
        verify { s3Port.generateDownloadPresignedUrl(pdfKey, any()) }
    }

    @Test
    fun `존재하지 않는 이력서 조회 시 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findDetailById(999L) } returns null

        val ex =
            assertFailsWith<BusinessException> {
                useCase.getDetail(GetResumeQuery(resumeId = 999L, userId = 1L))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `이미 삭제된 이력서 조회 시 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findDetailById(1L) } returns detailFixture(deleted = true)

        val ex =
            assertFailsWith<BusinessException> {
                useCase.getDetail(GetResumeQuery(resumeId = 1L, userId = 1L))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 이력서 조회 시 FORBIDDEN 예외가 발생한다`() {
        every { resumeRepository.findDetailById(1L) } returns detailFixture(userId = 1L)

        val ex =
            assertFailsWith<BusinessException> {
                useCase.getDetail(GetResumeQuery(resumeId = 1L, userId = 99L))
            }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
