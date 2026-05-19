package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.FeedbackListResult
import com.atomiccv.resume.application.usecase.GetAllFeedbacksUseCase
import com.atomiccv.resume.domain.model.Feedback
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime

@WebMvcTest(AllFeedbackController::class)
@Import(AllFeedbackControllerTest.MockConfig::class)
class AllFeedbackControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var getAllFeedbacksUseCase: GetAllFeedbacksUseCase

    private val feedback =
        Feedback(
            id = 1L,
            resumeId = 1L,
            rating = 4,
            comment = "좋습니다",
            reviewerIp = "1.2.3.4",
            tags = listOf("성과중심"),
            createdAt = LocalDateTime.of(2026, 5, 19, 10, 0),
        )

    @TestConfiguration
    class MockConfig {
        @Bean
        fun getAllFeedbacksUseCase(): GetAllFeedbacksUseCase = mockk()

        @Bean
        fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http
                .csrf { it.disable() }
                .authorizeHttpRequests {
                    it.requestMatchers(HttpMethod.POST, "/api/resumes/*/feedbacks").permitAll()
                    it.anyRequest().authenticated()
                }.exceptionHandling {
                    it.authenticationEntryPoint { _, response, _ ->
                        response.sendError(401, "Unauthorized")
                    }
                }
            return http.build()
        }
    }

    @Test
    @WithMockUser(username = "10")
    fun `GET api-feedbacks - 전체 이력서 피드백 목록을 반환한다`() {
        every { getAllFeedbacksUseCase.getAll(any()) } returns
            FeedbackListResult(feedbacks = listOf(feedback), totalCount = 1L)

        mockMvc.get("/api/feedbacks").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.content[0].id") { value(1) }
            jsonPath("$.data.content[0].resumeId") { value(1) }
            jsonPath("$.data.totalElements") { value(1) }
        }
    }

    @Test
    fun `GET api-feedbacks - 미인증 요청 시 401을 반환한다`() {
        mockMvc.get("/api/feedbacks").andExpect {
            status { isUnauthorized() }
        }
    }
}
