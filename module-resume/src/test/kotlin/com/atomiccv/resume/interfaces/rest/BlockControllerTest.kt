package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.domain.model.Block
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

@WebMvcTest(BlockController::class)
@Import(BlockControllerTest.MockConfig::class)
class BlockControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var createBlockUseCase: CreateBlockUseCase

    @Autowired
    lateinit var updateBlockUseCase: UpdateBlockUseCase

    @Autowired
    lateinit var deleteBlockUseCase: DeleteBlockUseCase

    @Autowired
    lateinit var getBlocksUseCase: GetBlocksUseCase

    private val block =
        Block(
            id = 1L,
            userId = 1L,
            type = BlockType.CAREER,
            title = "카카오 백엔드 개발자",
            contentJson = """{"company":"카카오"}""",
            createdAt = LocalDateTime.of(2026, 5, 11, 10, 0),
            updatedAt = LocalDateTime.of(2026, 5, 11, 10, 0),
        )

    @TestConfiguration
    class MockConfig {
        @Bean
        fun createBlockUseCase(): CreateBlockUseCase = mockk()

        @Bean
        fun updateBlockUseCase(): UpdateBlockUseCase = mockk()

        @Bean
        fun deleteBlockUseCase(): DeleteBlockUseCase = mockk()

        @Bean
        fun getBlocksUseCase(): GetBlocksUseCase = mockk()
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-blocks - 블록 목록을 반환한다`() {
        every { getBlocksUseCase.getBlocks(any()) } returns listOf(block)

        mockMvc.get("/api/blocks").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data[0].id") { value(1) }
            jsonPath("$.data[0].type") { value("CAREER") }
            jsonPath("$.data[0].title") { value("카카오 백엔드 개발자") }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `POST api-blocks - 블록을 생성하고 반환한다`() {
        every { createBlockUseCase.create(any()) } returns block

        mockMvc
            .post("/api/blocks") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("type" to "CAREER", "title" to "카카오 백엔드 개발자", "contentJson" to """{"company":"카카오"}"""),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-blocks-id - 블록을 수정하고 반환한다`() {
        every { updateBlockUseCase.update(any()) } returns block

        mockMvc
            .put("/api/blocks/1") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("title" to "수정 제목", "contentJson" to "{}"),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-blocks-id - 블록을 삭제하고 success를 반환한다`() {
        every { deleteBlockUseCase.delete(1L, 1L) } just runs

        mockMvc
            .delete("/api/blocks/1") {
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
    }
}
