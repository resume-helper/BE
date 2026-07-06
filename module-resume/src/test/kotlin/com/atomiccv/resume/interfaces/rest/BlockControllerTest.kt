package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.BlockCounts
import com.atomiccv.resume.application.usecase.BlockTypeCount
import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.GetBlockCountsUseCase
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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

    @Autowired
    lateinit var getBlockCountsUseCase: GetBlockCountsUseCase

    private val careerContentJson =
        mapOf(
            "companyName" to "카카오",
            "department" to "서버개발팀",
            "jobTitle" to "백엔드 개발자",
            "position" to "사원",
            "employmentType" to "FULL_TIME",
            "startDate" to "2024.03",
            "endDate" to "2026.01",
            "achievements" to "서비스 안정성 개선",
        )

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

        @Bean
        fun getBlockCountsUseCase(): GetBlockCountsUseCase = mockk()
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-blocks - 블록 목록을 페이지로 반환한다`() {
        val pageResult = PageImpl(listOf(block), PageRequest.of(0, 20), 1)
        every { getBlocksUseCase.getBlocks(any()) } returns pageResult

        mockMvc.get("/api/blocks").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.content[0].id") { value(1) }
            jsonPath("$.data.content[0].type") { value("CAREER") }
            jsonPath("$.data.content[0].title") { value("카카오 백엔드 개발자") }
            jsonPath("$.data.totalElements") { value(1) }
            jsonPath("$.data.hasNext") { value(false) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `POST api-blocks - 블록 리스트를 생성하고 반환한다`() {
        every { createBlockUseCase.create(any()) } returns listOf(block)
        val blockItem = mapOf("blockType" to "CAREER", "title" to "카카오 백엔드 개발자", "contentJson" to careerContentJson)
        val requestBody = mapOf("blocks" to listOf(blockItem))

        mockMvc
            .post("/api/blocks") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody)
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data[0].id") { value(1) }
                jsonPath("$.data[0].type") { value("CAREER") }
                jsonPath("$.data[0].title") { value("카카오 백엔드 개발자") }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-blocks-id - 블록을 수정하고 반환한다`() {
        every { updateBlockUseCase.update(any()) } returns block
        val requestBody = mapOf("blockType" to "CAREER", "title" to "수정 제목", "contentJson" to careerContentJson)

        mockMvc
            .put("/api/blocks/1") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody)
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

    @Test
    @WithMockUser(username = "1")
    fun `GET api-blocks-counts - 타입별 블록 개수를 반환한다`() {
        val counts =
            BlockCounts(
                totalCount = 3L,
                counts =
                    listOf(
                        BlockTypeCount(BlockType.BASIC_INFO, 0L),
                        BlockTypeCount(BlockType.CAREER, 3L),
                    ),
            )
        every { getBlockCountsUseCase.getCounts(1L) } returns counts

        mockMvc.get("/api/blocks/counts").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.totalCount") { value(3) }
            jsonPath("$.data.counts[0].type") { value("BASIC_INFO") }
            jsonPath("$.data.counts[0].count") { value(0) }
            jsonPath("$.data.counts[1].type") { value("CAREER") }
            jsonPath("$.data.counts[1].count") { value(3) }
        }
    }

    @Test
    fun `GET api-blocks-counts - 미인증 요청 시 401을 반환한다`() {
        mockMvc.get("/api/blocks/counts").andExpect {
            status { isUnauthorized() }
        }
    }
}
