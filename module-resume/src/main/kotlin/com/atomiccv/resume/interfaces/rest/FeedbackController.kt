package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.DeleteFeedbackUseCase
import com.atomiccv.resume.application.usecase.FeedbackListResult
import com.atomiccv.resume.application.usecase.GetFeedbackListQuery
import com.atomiccv.resume.application.usecase.GetFeedbackListUseCase
import com.atomiccv.resume.application.usecase.GetFeedbackQuery
import com.atomiccv.resume.application.usecase.GetFeedbackUseCase
import com.atomiccv.resume.application.usecase.SubmitFeedbackCommand
import com.atomiccv.resume.application.usecase.SubmitFeedbackUseCase
import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import com.atomiccv.shared.common.response.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import kotlin.math.ceil
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Feedback", description = "피드백 API — 제출·조회·삭제")
@RestController
@RequestMapping("/api/resumes/{resumeId}/feedbacks")
class FeedbackController(
    private val submitFeedbackUseCase: SubmitFeedbackUseCase,
    private val getFeedbackListUseCase: GetFeedbackListUseCase,
    private val getFeedbackUseCase: GetFeedbackUseCase,
    private val deleteFeedbackUseCase: DeleteFeedbackUseCase,
) {
    @Operation(
        summary = "피드백 제출",
        description = "공개된 이력서에 피드ㅌ백을 제출합니다. 인증 없이 접근 가능합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "제출 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "입력값 유효성 검증 실패 (VALIDATION_FAILED)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "이력서 소유자는 피드백 제출 불가 (FORBIDDEN)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서를 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
    )
    @PostMapping
    fun submitFeedback(
        @PathVariable resumeId: Long,
        @Valid @RequestBody request: SubmitFeedbackRequest,
        servletRequest: HttpServletRequest,
        authentication: Authentication?,
    ): ResponseEntity<ApiResponse<FeedbackResponse>> {
        val feedback =
            submitFeedbackUseCase.submit(
                SubmitFeedbackCommand(
                    resumeId = resumeId,
                    rating = request.rating,
                    comment = request.comment,
                    tags = request.tags,
                    reviewerIp = extractClientIp(servletRequest),
                    requestUserId = authentication?.name?.toLongOrNull(),
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(feedback.toResponse()))
    }

    @Operation(
        summary = "피드백 목록 조회",
        description = "이력서 소유자가 피드백 목록을 페이지네이션으로 조회합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "403",
            description = "이력서 소유자가 아닌 경우 (FORBIDDEN)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서를 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
    )
    @GetMapping
    fun getFeedbackList(
        authentication: Authentication,
        @PathVariable resumeId: Long,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<ApiResponse<PageResponse<FeedbackResponse>>> {
        val userId = resolveUserId(authentication)
        val result =
            getFeedbackListUseCase.getList(
                GetFeedbackListQuery(
                    resumeId = resumeId,
                    requestUserId = userId,
                    page = page,
                    size = size,
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(result.toPageResponse(page, size)))
    }

    @Operation(
        summary = "피드백 단건 조회",
        description = "이력서 소유자가 특정 피드백을 단건 조회합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "403",
            description = "이력서 소유자가 아닌 경우 (FORBIDDEN)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "피드백을 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
    )
    @GetMapping("/{feedbackId}")
    fun getFeedback(
        authentication: Authentication,
        @PathVariable resumeId: Long,
        @PathVariable feedbackId: Long,
    ): ResponseEntity<ApiResponse<FeedbackResponse>> {
        val userId = resolveUserId(authentication)
        val feedback =
            getFeedbackUseCase.get(
                GetFeedbackQuery(
                    resumeId = resumeId,
                    feedbackId = feedbackId,
                    requestUserId = userId,
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(feedback.toResponse()))
    }

    @Operation(
        summary = "피드백 삭제",
        description = "이력서 소유자가 피드백을 삭제합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "삭제 성공"),
        SwaggerApiResponse(
            responseCode = "403",
            description = "이력서 소유자가 아닌 경우 (FORBIDDEN)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "피드백을 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ErrorResponse")),
            ],
        ),
    )
    @DeleteMapping("/{feedbackId}")
    fun deleteFeedback(
        authentication: Authentication,
        @PathVariable resumeId: Long,
        @PathVariable feedbackId: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteFeedbackUseCase.delete(
            resumeId = resumeId,
            feedbackId = feedbackId,
            requestUserId = userId,
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

    private fun extractClientIp(request: HttpServletRequest): String =
        request
            .getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: request.remoteAddr
}

@Schema(description = "피드백 제출 요청")
data class SubmitFeedbackRequest(
    @Schema(description = "별점 (1~5)", example = "4")
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    @Schema(description = "텍스트 코멘트 (선택)", example = "경력 설명이 인상적이었습니다.")
    @field:Size(max = 1000)
    val comment: String?,
    @Schema(description = "태그 목록 (선택)", example = "[\"성과중심\", \"간결함\"]")
    val tags: List<
        @Size(max = 50)
        String
    > = emptyList(),
)

@Schema(description = "피드백 응답")
data class FeedbackResponse(
    @Schema(description = "피드백 ID", example = "1")
    val id: Long,
    @Schema(description = "이력서 ID", example = "1")
    val resumeId: Long,
    @Schema(description = "별점 (1~5)", example = "4")
    val rating: Int,
    @Schema(description = "텍스트 코멘트", example = "경력 설명이 인상적이었습니다.")
    val comment: String?,
    @Schema(description = "태그 목록", example = "[\"성과중심\"]")
    val tags: List<String>,
    @Schema(description = "제출 일시", example = "2026-05-19T10:00:00")
    val createdAt: LocalDateTime,
)

fun Feedback.toResponse() =
    FeedbackResponse(
        id = id,
        resumeId = resumeId,
        rating = rating,
        comment = comment,
        tags = tags,
        createdAt = createdAt,
    )

fun FeedbackListResult.toPageResponse(
    page: Int,
    size: Int,
): PageResponse<FeedbackResponse> =
    PageResponse(
        content = feedbacks.map { it.toResponse() },
        page = page,
        size = size,
        totalElements = totalCount,
        totalPages = if (totalCount == 0L) 1 else ceil(totalCount.toDouble() / size).toInt(),
        hasNext = page * size < totalCount,
        hasPrevious = page > 1,
    )
