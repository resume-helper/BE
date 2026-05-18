package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.ReorderBlocksUseCase
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.interfaces.rest.GlobalExceptionHandler
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
import org.springframework.test.web.servlet.put

@WebMvcTest(ResumeBlockController::class)
@Import(GlobalExceptionHandler::class, ResumeBlockControllerTest.MockConfig::class)
class ResumeBlockControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var reorderBlocksUseCase: ReorderBlocksUseCase

    @TestConfiguration
    class MockConfig {
        @Bean
        fun reorderBlocksUseCase(): ReorderBlocksUseCase = mockk()
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-resumes-resumeId-blocks-order - 블록 순서를 변경하고 success를 반환한다`() {
        every { reorderBlocksUseCase.reorder(any()) } just runs

        mockMvc
            .put("/api/resumes/1/blocks/order") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("blockIds" to listOf(3, 1, 2)))
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-resumes-resumeId-blocks-order - 이력서가 없으면 404를 반환한다`() {
        every { reorderBlocksUseCase.reorder(any()) } throws
            BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")

        mockMvc
            .put("/api/resumes/999/blocks/order") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("blockIds" to listOf(1)))
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-resumes-resumeId-blocks-order - 타인의 이력서면 403을 반환한다`() {
        every { reorderBlocksUseCase.reorder(any()) } throws BusinessException(ErrorCode.FORBIDDEN)

        mockMvc
            .put("/api/resumes/1/blocks/order") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("blockIds" to listOf(1, 2)))
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-resumes-resumeId-blocks-order - 블록 목록 불일치 시 400을 반환한다`() {
        every { reorderBlocksUseCase.reorder(any()) } throws
            BusinessException(ErrorCode.VALIDATION_FAILED, "블록 목록이 이력서의 블록 목록과 일치하지 않습니다.")

        mockMvc
            .put("/api/resumes/1/blocks/order") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("blockIds" to listOf(1, 999)))
            }.andExpect {
                status { isBadRequest() }
            }
    }
}
