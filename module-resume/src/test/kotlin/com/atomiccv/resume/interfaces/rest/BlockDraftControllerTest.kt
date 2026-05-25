package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.BlockDraftSummary
import com.atomiccv.resume.application.usecase.CreateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockDraftUseCase
import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
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
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime

@WebMvcTest(BlockDraftController::class)
@Import(BlockDraftControllerTest.MockConfig::class)
class BlockDraftControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @Autowired lateinit var createBlockDraftUseCase: CreateBlockDraftUseCase

    @Autowired lateinit var updateBlockDraftUseCase: UpdateBlockDraftUseCase

    @Autowired lateinit var getBlockDraftsUseCase: GetBlockDraftsUseCase

    @Autowired lateinit var getBlockDraftUseCase: GetBlockDraftUseCase

    @Autowired lateinit var deleteBlockDraftUseCase: DeleteBlockDraftUseCase

    @Autowired lateinit var deleteBlockDraftsUseCase: DeleteBlockDraftsUseCase

    @Autowired lateinit var deleteAllBlockDraftsByTypeUseCase: DeleteAllBlockDraftsByTypeUseCase

    @TestConfiguration
    class MockConfig {
        @Bean fun createBlockDraftUseCase(): CreateBlockDraftUseCase = mockk()

        @Bean fun updateBlockDraftUseCase(): UpdateBlockDraftUseCase = mockk()

        @Bean fun getBlockDraftsUseCase(): GetBlockDraftsUseCase = mockk()

        @Bean fun getBlockDraftUseCase(): GetBlockDraftUseCase = mockk()

        @Bean fun deleteBlockDraftUseCase(): DeleteBlockDraftUseCase = mockk()

        @Bean fun deleteBlockDraftsUseCase(): DeleteBlockDraftsUseCase = mockk()

        @Bean fun deleteAllBlockDraftsByTypeUseCase(): DeleteAllBlockDraftsByTypeUseCase = mockk()
    }

    private val draft =
        BlockDraft(
            id = 1L,
            userId = 1L,
            blockType = BlockType.CAREER,
            title = "카카오 백엔드 개발자",
            contentJson = """{"company":"카카오"}""",
            expiresAt = LocalDateTime.of(2026, 6, 5, 14, 30),
            createdAt = LocalDateTime.of(2026, 5, 22, 14, 30),
            updatedAt = LocalDateTime.of(2026, 5, 22, 14, 30),
        )

    @Test
    @WithMockUser(username = "1")
    fun `POST api-block-drafts - 임시저장을 생성하고 반환한다`() {
        every { createBlockDraftUseCase.create(any()) } returns draft

        mockMvc
            .post("/api/block-drafts") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf(
                            "blockType" to "CAREER",
                            "title" to "카카오 백엔드 개발자",
                            "contentJson" to mapOf("company" to "카카오"),
                        ),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.blockType") { value("CAREER") }
                jsonPath("$.data.title") { value("카카오 백엔드 개발자") }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-block-drafts - 블록 타입별 목록을 반환한다`() {
        val summary =
            BlockDraftSummary(
                id = 1L,
                title = "카카오 백엔드 개발자",
                updatedAt = LocalDateTime.of(2026, 5, 22, 14, 30),
                isActive = true,
            )
        every { getBlockDraftsUseCase.getDrafts(any()) } returns listOf(summary)

        mockMvc.get("/api/block-drafts?type=CAREER&currentDraftId=1").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data[0].id") { value(1) }
            jsonPath("$.data[0].isActive") { value(true) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-block-drafts-id - 단건 드래프트를 반환한다`() {
        every { getBlockDraftUseCase.getDraft(any()) } returns draft

        mockMvc.get("/api/block-drafts/1").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.id") { value(1) }
            jsonPath("$.data.title") { value("카카오 백엔드 개발자") }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-block-drafts-id - 드래프트를 덮어쓰고 반환한다`() {
        every { updateBlockDraftUseCase.update(any()) } returns draft

        mockMvc
            .put("/api/block-drafts/1") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("title" to "수정 제목", "contentJson" to emptyMap<String, Any>()),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-block-drafts-id - 단건 삭제 후 success를 반환한다`() {
        every { deleteBlockDraftUseCase.delete(any()) } just runs

        mockMvc.delete("/api/block-drafts/1") { with(csrf()) }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-block-drafts-batch - 선택 삭제 후 success를 반환한다`() {
        every { deleteBlockDraftsUseCase.deleteAll(any()) } just runs

        mockMvc
            .delete("/api/block-drafts/batch") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("ids" to listOf(1L, 2L)))
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-block-drafts with type - 타입별 전체 삭제 후 success를 반환한다`() {
        every { deleteAllBlockDraftsByTypeUseCase.deleteAll(any()) } just runs

        mockMvc.delete("/api/block-drafts?type=CAREER") { with(csrf()) }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }
}
