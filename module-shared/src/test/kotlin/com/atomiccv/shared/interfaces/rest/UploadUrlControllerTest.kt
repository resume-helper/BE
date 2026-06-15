package com.atomiccv.shared.interfaces.rest

import com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCase
import com.atomiccv.shared.application.usecase.UploadUrlResult
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(UploadUrlController::class)
@Import(UploadUrlControllerTest.MockConfig::class)
class UploadUrlControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var generateUploadUrlUseCase: GenerateUploadUrlUseCase

    @TestConfiguration
    class MockConfig {
        @Bean
        fun generateUploadUrlUseCase(): GenerateUploadUrlUseCase = mockk()
    }

    @Test
    @WithMockUser(username = "1")
    fun `POST api-upload-url - S3 presigned URL을 발급하고 반환한다`() {
        every { generateUploadUrlUseCase.generate(any()) } returns
            UploadUrlResult(
                presignedUrl = "https://upload.url",
                s3Key = "resumes/1/uuid/file.pdf",
            )

        mockMvc
            .post("/api/upload-url") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("fileName" to "resume.pdf"),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.presignedUrl") { value("https://upload.url") }
                jsonPath("$.data.s3Key") { value("resumes/1/uuid/file.pdf") }
            }
    }

    @Test
    fun `POST api-upload-url - 미인증 요청 시 401을 반환한다`() {
        mockMvc
            .post("/api/upload-url") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("fileName" to "resume.pdf"),
                    )
            }.andExpect {
                status { isUnauthorized() }
            }
    }
}
