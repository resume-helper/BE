package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.GetResumeAnalyticsQuery
import com.atomiccv.resume.application.usecase.GetResumeAnalyticsUseCase
import com.atomiccv.resume.application.usecase.ResumeAnalyticsResult
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Resume Analytics", description = "이력서 열람 분석 API — 소유자 전용")
@RestController
@RequestMapping("/api/resumes/{resumeId}/analytics")
class ResumeAnalyticsController(
    private val getResumeAnalyticsUseCase: GetResumeAnalyticsUseCase,
) {
    @Operation(
        summary = "열람 분석 조회",
        description = "총열람 수, 고유 방문자(IP 기준), 최근 7일·직전 7일 비교창, 섹션별 평균 체류시간을 반환합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "403",
            description = "이력서 소유자가 아닌 경우 (FORBIDDEN)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ApiResponse")),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서를 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ApiResponse")),
            ],
        ),
    )
    @GetMapping
    fun getAnalytics(
        authentication: Authentication,
        @PathVariable resumeId: Long,
    ): ResponseEntity<ApiResponse<ResumeAnalyticsResponse>> {
        val userId =
            authentication.name.toLongOrNull()
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        val result =
            getResumeAnalyticsUseCase.getAnalytics(
                GetResumeAnalyticsQuery(resumeId = resumeId, requestUserId = userId),
            )
        return ResponseEntity.ok(ApiResponse.ok(result.toResponse()))
    }
}

// ── Responses ─────────────────────────────────────────────────────────────────

@Schema(description = "이력서 열람 분석 응답")
data class ResumeAnalyticsResponse(
    @Schema(description = "총열람 수 (누적)", example = "100")
    val totalViews: Long,
    @Schema(description = "고유 방문자 수 (IP 기준)", example = "42")
    val uniqueVisitors: Long,
    @Schema(description = "최근 7일 집계")
    val last7Days: ViewAggregateResponse,
    @Schema(description = "직전 7일(8~14일 전) 집계 — 증감 표시용")
    val previous7Days: ViewAggregateResponse,
    @Schema(description = "섹션별 평균 체류시간")
    val sectionDwells: List<SectionDwellAverageResponse>,
)

@Schema(description = "기간 집계")
data class ViewAggregateResponse(
    @Schema(description = "조회수", example = "30")
    val viewCount: Long,
    @Schema(description = "평균 체류시간(초). 보고된 세션이 없으면 null", nullable = true, example = "90.0")
    val averageDurationSec: Double?,
)

@Schema(description = "섹션별 평균 체류시간")
data class SectionDwellAverageResponse(
    @Schema(description = "섹션", example = "CAREER")
    val section: BlockType,
    @Schema(description = "평균 체류시간(초)", example = "108.0")
    val averageDwellSec: Double,
)

fun ResumeAnalyticsResult.toResponse() =
    ResumeAnalyticsResponse(
        totalViews = totalViews,
        uniqueVisitors = uniqueVisitors,
        last7Days = ViewAggregateResponse(last7Days.viewCount, last7Days.averageDurationSec),
        previous7Days = ViewAggregateResponse(previous7Days.viewCount, previous7Days.averageDurationSec),
        sectionDwells = sectionDwells.map { SectionDwellAverageResponse(it.section, it.averageDwellSec) },
    )
