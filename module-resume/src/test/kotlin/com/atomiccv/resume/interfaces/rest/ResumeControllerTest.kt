package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.CreateResumeUseCase
import com.atomiccv.resume.application.usecase.DeleteResumeUseCase
import com.atomiccv.resume.application.usecase.GetResumeUseCase
import com.atomiccv.resume.application.usecase.GetResumesUseCase
import com.atomiccv.resume.application.usecase.UpdateResumeUseCase
import com.atomiccv.resume.application.usecase.UpdateResumeVisibilityUseCase
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.ResumeBlockDetail
import com.atomiccv.resume.domain.repository.ResumeDetail
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime

@WebMvcTest(ResumeController::class)
@Import(ResumeControllerTest.MockConfig::class)
class ResumeControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var createResumeUseCase: CreateResumeUseCase

    @Autowired
    lateinit var getResumesUseCase: GetResumesUseCase

    @Autowired
    lateinit var getResumeUseCase: GetResumeUseCase

    @Autowired
    lateinit var updateResumeUseCase: UpdateResumeUseCase

    @Autowired
    lateinit var deleteResumeUseCase: DeleteResumeUseCase

    @Autowired
    lateinit var updateResumeVisibilityUseCase: UpdateResumeVisibilityUseCase

    @TestConfiguration
    class MockConfig {
        @Bean
        fun createResumeUseCase(): CreateResumeUseCase = mockk()

        @Bean
        fun getResumesUseCase(): GetResumesUseCase = mockk()

        @Bean
        fun getResumeUseCase(): GetResumeUseCase = mockk()

        @Bean
        fun updateResumeUseCase(): UpdateResumeUseCase = mockk()

        @Bean
        fun deleteResumeUseCase(): DeleteResumeUseCase = mockk()

        @Bean
        fun updateResumeVisibilityUseCase(): UpdateResumeVisibilityUseCase = mockk()
    }

    private fun resumeFixture() =
        Resume(
            id = 1L,
            userId = 1L,
            title = "테스트 이력서",
            type = ResumeType.WEB,
            slug = "abc123",
            isPublic = false,
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

    private fun detailFixture() =
        ResumeDetail(
            resume = resumeFixture(),
            blocks =
                listOf(
                    ResumeBlockDetail(
                        blockId = 10L,
                        orderIndex = 0,
                        title = "블록1",
                        type = BlockType.CAREER,
                        contentJson = "{}",
                    ),
                ),
        )

    @Test
    @WithMockUser(username = "1")
    fun `POST api-resumes - 이력서를 생성하고 반환한다`() {
        every { createResumeUseCase.create(any()) } returns resumeFixture()

        mockMvc
            .post("/api/resumes") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("title" to "테스트 이력서", "type" to "WEB", "blocks" to emptyList<Any>()),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.title") { value("테스트 이력서") }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-resumes - PageResponse 형식으로 목록을 반환한다`() {
        every { getResumesUseCase.getList(any()) } returns
            PageImpl(listOf(resumeFixture()), PageRequest.of(0, 10), 1)

        mockMvc.get("/api/resumes").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.page") { value(1) }
            jsonPath("$.data.totalElements") { value(1) }
            jsonPath("$.data.content[0].id") { value(1) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-resumes-id - WEB 이력서 조회 시 pdfDownloadUrl이 null이다`() {
        every { getResumeUseCase.getDetail(any()) } returns detailFixture()

        mockMvc.get("/api/resumes/1").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.pdfDownloadUrl") { doesNotExist() }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-resumes-id - PDF 이력서 조회 시 pdfDownloadUrl이 포함된다`() {
        val pdfDetail =
            ResumeDetail(
                resume = resumeFixture().copy(pdfS3Key = "resumes/1/uuid/file.pdf"),
                blocks = detailFixture().blocks,
            )
        every { getResumeUseCase.getDetail(any()) } returns pdfDetail
        every { getResumeUseCase.getPresignedDownloadUrl(any()) } returns "https://s3.example.com/signed"

        mockMvc.get("/api/resumes/1").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.pdfDownloadUrl") { value("https://s3.example.com/signed") }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-resumes-id - 이력서를 수정하고 반환한다`() {
        every { updateResumeUseCase.update(any()) } returns resumeFixture()

        mockMvc
            .put("/api/resumes/1") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("title" to "수정된 제목", "blocks" to emptyList<Any>()),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.title") { value("테스트 이력서") }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-resumes - 이력서를 일괄 삭제하고 success를 반환한다`() {
        every { deleteResumeUseCase.deleteAll(any(), any()) } just runs

        mockMvc
            .delete("/api/resumes") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("ids" to listOf(1L, 2L)),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PATCH api-resumes-id-visibility - 이력서 공개 여부를 변경하고 반환한다`() {
        every { updateResumeVisibilityUseCase.update(any()) } returns resumeFixture()

        mockMvc
            .patch("/api/resumes/1/visibility") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("isPublic" to true),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
            }
    }

    @Test
    fun `POST api-resumes - 미인증 요청 시 401을 반환한다`() {
        mockMvc
            .post("/api/resumes") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("title" to "테스트 이력서", "type" to "WEB", "blocks" to emptyList<Any>()),
                    )
            }.andExpect {
                status { isUnauthorized() }
            }
    }
}
