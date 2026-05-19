package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.GetAllFeedbacksQuery
import com.atomiccv.resume.application.usecase.GetAllFeedbacksUseCase
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import com.atomiccv.shared.common.response.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/api/feedbacks")
class AllFeedbackController(
    private val getAllFeedbacksUseCase: GetAllFeedbacksUseCase,
) {
    @Operation(
        summary = "전체 이력서 피드백 목록 조회",
        description = "인증된 사용자의 모든 이력서에 걸친 피드백을 최신순으로 조회합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증 필요 (UNAUTHORIZED)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
    )
    @GetMapping
    fun getAllFeedbacks(
        authentication: Authentication,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<ApiResponse<PageResponse<FeedbackResponse>>> {
        val userId =
            authentication.name.toLongOrNull()
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        val result =
            getAllFeedbacksUseCase.getAll(
                GetAllFeedbacksQuery(requestUserId = userId, page = page, size = size),
            )
        return ResponseEntity.ok(ApiResponse.ok(result.toPageResponse(page, size)))
    }
}
