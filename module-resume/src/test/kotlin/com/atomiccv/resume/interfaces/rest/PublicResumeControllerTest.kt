package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.GetPublicResumeUseCase
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeBlockDetail
import com.atomiccv.resume.domain.repository.ResumeDetail
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.interfaces.rest.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime

@WebMvcTest(PublicResumeController::class)
@Import(PublicResumeControllerTest.MockConfig::class, GlobalExceptionHandler::class)
class PublicResumeControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var getPublicResumeUseCase: GetPublicResumeUseCase

    @TestConfiguration
    class MockConfig {
        @Bean
        fun getPublicResumeUseCase(): GetPublicResumeUseCase = mockk()

        @Bean
        fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http
                .csrf { it.disable() }
                .authorizeHttpRequests {
                    it.requestMatchers(HttpMethod.GET, "/api/public/resumes/*").permitAll()
                    it.anyRequest().authenticated()
                }
            return http.build()
        }
    }

    private fun detailFixture() =
        ResumeDetail(
            resume =
                Resume(
                    id = 1L,
                    userId = 7L,
                    title = "공개 이력서",
                    slug = "abc123",
                    isPublic = true,
                    pdfS3Key = "resumes/7/secret.pdf",
                    updatedAt = LocalDateTime.of(2026, 7, 1, 10, 0),
                ),
            blocks =
                listOf(
                    ResumeBlockDetail(
                        blockId = 11L,
                        orderIndex = 0,
                        title = "백엔드 개발자",
                        type = BlockType.CAREER,
                        contentJson = "{}",
                    ),
                ),
        )

    @Test
    fun `GET api-public-resumes-slug - 인증 없이 공개 이력서 상세를 반환한다`() {
        every { getPublicResumeUseCase.getDetail(match { it.slug == "abc123" }) } returns detailFixture()

        mockMvc
            .get("/api/public/resumes/abc123")
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.title") { value("공개 이력서") }
                jsonPath("$.data.blocks[0].blockId") { value(11) }
                jsonPath("$.data.blocks[0].type") { value("CAREER") }
            }
    }

    @Test
    fun `GET api-public-resumes-slug - 소유자 전용 필드는 응답에 포함하지 않는다`() {
        every { getPublicResumeUseCase.getDetail(any()) } returns detailFixture()

        mockMvc
            .get("/api/public/resumes/abc123")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.pdfS3Key") { doesNotExist() }
                jsonPath("$.data.isPublic") { doesNotExist() }
                jsonPath("$.data.userId") { doesNotExist() }
            }
    }

    @Test
    fun `GET api-public-resumes-slug - 비공개·부재 이력서는 404를 반환한다`() {
        every { getPublicResumeUseCase.getDetail(any()) } throws BusinessException(ErrorCode.RESUME_NOT_FOUND)

        mockMvc
            .get("/api/public/resumes/hidden")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.success") { value(false) }
            }
    }
}
