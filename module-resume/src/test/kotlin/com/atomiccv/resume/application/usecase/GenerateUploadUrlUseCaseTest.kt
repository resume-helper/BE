package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.application.port.S3Port
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateUploadUrlUseCaseTest {
    private val s3Port: S3Port = mockk()
    private val useCase = GenerateUploadUrlUseCase(s3Port)

    @Test
    fun `presigned URL 발급 시 s3Key에 userId가 포함된다`() {
        every { s3Port.generateUploadPresignedUrl(any(), any()) } returns "https://s3.example.com/test"

        val result = useCase.generate(GenerateUploadUrlCommand(userId = 42L, fileName = "resume.pdf"))

        assertTrue(result.s3Key.contains("42"))
    }

    @Test
    fun `반환된 s3Key 형식이 resumes_{userId}_로 시작한다`() {
        every { s3Port.generateUploadPresignedUrl(any(), any()) } returns "https://s3.example.com/test"

        val result = useCase.generate(GenerateUploadUrlCommand(userId = 7L, fileName = "my-cv.pdf"))

        assertTrue(result.s3Key.startsWith("resumes/7/"))
    }

    @Test
    fun `presignedUrl은 s3Port에서 반환된 값과 동일하다`() {
        val expectedUrl = "https://s3.example.com/presigned-url"
        every { s3Port.generateUploadPresignedUrl(any(), any()) } returns expectedUrl

        val result = useCase.generate(GenerateUploadUrlCommand(userId = 1L, fileName = "resume.pdf"))

        assertEquals(expectedUrl, result.presignedUrl)
    }

    @Test
    fun `s3Key에 fileName이 포함된다`() {
        every { s3Port.generateUploadPresignedUrl(any(), any()) } returns "https://s3.example.com/test"

        val result = useCase.generate(GenerateUploadUrlCommand(userId = 1L, fileName = "my-resume.pdf"))

        assertTrue(result.s3Key.contains("my-resume.pdf"))
    }
}
