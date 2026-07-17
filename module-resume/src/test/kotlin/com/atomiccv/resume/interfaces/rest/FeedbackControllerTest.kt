package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.DeleteFeedbackUseCase
import com.atomiccv.resume.application.usecase.FeedbackGroupStats
import com.atomiccv.resume.application.usecase.FeedbackListResult
import com.atomiccv.resume.application.usecase.FeedbackStatsResult
import com.atomiccv.resume.application.usecase.GetFeedbackListUseCase
import com.atomiccv.resume.application.usecase.GetFeedbackStatsUseCase
import com.atomiccv.resume.application.usecase.GetFeedbackUseCase
import com.atomiccv.resume.application.usecase.SectionFeedbackStats
import com.atomiccv.resume.application.usecase.SubmitFeedbackUseCase
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Feedback
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@WebMvcTest(FeedbackController::class)
@Import(FeedbackControllerTest.MockConfig::class)
class FeedbackControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var submitFeedbackUseCase: SubmitFeedbackUseCase

    @Autowired
    lateinit var getFeedbackListUseCase: GetFeedbackListUseCase

    @Autowired
    lateinit var getFeedbackUseCase: GetFeedbackUseCase

    @Autowired
    lateinit var getFeedbackStatsUseCase: GetFeedbackStatsUseCase

    @Autowired
    lateinit var deleteFeedbackUseCase: DeleteFeedbackUseCase

    private val feedback =
        Feedback(
            id = 1L,
            resumeId = 1L,
            rating = 4.0,
            comment = "좋습니다",
            reviewerIp = "1.2.3.4",
            tags = listOf("성과중심"),
            createdAt = LocalDateTime.of(2026, 5, 19, 10, 0),
        )

    @TestConfiguration
    class MockConfig {
        @Bean
        fun submitFeedbackUseCase(): SubmitFeedbackUseCase = mockk()

        @Bean
        fun getFeedbackListUseCase(): GetFeedbackListUseCase = mockk()

        @Bean
        fun getFeedbackUseCase(): GetFeedbackUseCase = mockk()

        @Bean
        fun getFeedbackStatsUseCase(): GetFeedbackStatsUseCase = mockk()

        @Bean
        fun deleteFeedbackUseCase(): DeleteFeedbackUseCase = mockk()

        @Bean
        fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http
                .csrf { it.disable() }
                .authorizeHttpRequests {
                    it.requestMatchers(HttpMethod.POST, "/api/resumes/*/feedbacks").permitAll()
                    it.anyRequest().authenticated()
                }
            return http.build()
        }
    }

    @Test
    fun `POST api-resumes-resumeId-feedbacks - 피드백을 제출하고 반환한다`() {
        every { submitFeedbackUseCase.submit(any()) } returns feedback

        mockMvc
            .post("/api/resumes/1/feedbacks") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("rating" to 4, "comment" to "좋습니다", "tags" to listOf("성과중심")),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.rating") { value(4.0) }
                jsonPath("$.data.tags[0]") { value("성과중심") }
            }
    }

    @Test
    @WithMockUser(username = "10")
    fun `POST api-resumes-resumeId-feedbacks - 로그인 사용자가 제출 시 requestUserId가 전달된다`() {
        every { submitFeedbackUseCase.submit(any()) } returns feedback

        mockMvc
            .post("/api/resumes/1/feedbacks") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("rating" to 4))
            }.andExpect {
                status { isOk() }
            }

        verify { submitFeedbackUseCase.submit(match { it.requestUserId == 10L }) }
    }

    @Test
    @WithMockUser(username = "10")
    fun `GET api-resumes-resumeId-feedbacks - 피드백 목록을 반환한다`() {
        every { getFeedbackListUseCase.getList(any()) } returns
            FeedbackListResult(feedbacks = listOf(feedback), totalCount = 1L)

        mockMvc.get("/api/resumes/1/feedbacks").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.content[0].id") { value(1) }
            jsonPath("$.data.totalElements") { value(1) }
        }
    }

    @Test
    @WithMockUser(username = "10")
    fun `GET api-resumes-resumeId-feedbacks-stats - 피드백 통계를 반환한다`() {
        every { getFeedbackStatsUseCase.getStats(any()) } returns
            FeedbackStatsResult(
                totalCount = 3L,
                overall =
                    FeedbackGroupStats(
                        count = 2L,
                        averageRating = 4.5,
                        tagCounts = mapOf("전체적으로 잘 읽혀요" to 2L),
                    ),
                sections =
                    listOf(
                        SectionFeedbackStats(
                            section = BlockType.CAREER,
                            count = 1L,
                            averageRating = 3.5,
                            tagCounts = emptyMap(),
                        ),
                    ),
            )

        mockMvc.get("/api/resumes/1/feedbacks/stats").andExpect {
            status { isOk() }
            jsonPath("$.data.totalCount") { value(3) }
            jsonPath("$.data.overall.averageRating") { value(4.5) }
            jsonPath("$.data.sections[0].section") { value("CAREER") }
        }
    }

    @Test
    @WithMockUser(username = "10")
    fun `GET api-resumes-resumeId-feedbacks-feedbackId - 피드백 단건을 반환한다`() {
        every { getFeedbackUseCase.get(any()) } returns feedback

        mockMvc.get("/api/resumes/1/feedbacks/1").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.id") { value(1) }
            jsonPath("$.data.rating") { value(4.0) }
        }
    }

    @Test
    @WithMockUser(username = "10")
    fun `DELETE api-resumes-resumeId-feedbacks-feedbackId - 피드백을 삭제하고 success를 반환한다`() {
        every { deleteFeedbackUseCase.delete(1L, 1L, 10L) } just runs

        mockMvc
            .delete("/api/resumes/1/feedbacks/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
    }
}
