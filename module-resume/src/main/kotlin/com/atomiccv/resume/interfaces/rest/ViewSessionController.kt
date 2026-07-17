package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.RecordViewDurationCommand
import com.atomiccv.resume.application.usecase.RecordViewDurationUseCase
import com.atomiccv.resume.application.usecase.StartViewSessionCommand
import com.atomiccv.resume.application.usecase.StartViewSessionUseCase
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.SectionDwell
import com.atomiccv.shared.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Public Resume", description = "공개 웹 이력서 열람 API — 비로그인 외부 열람자용")
@RestController
@RequestMapping("/api/public/resumes/{slug}/view-sessions")
class ViewSessionController(
    private val startViewSessionUseCase: StartViewSessionUseCase,
    private val recordViewDurationUseCase: RecordViewDurationUseCase,
) {
    @Operation(
        summary = "열람 세션 시작",
        description = "공개 이력서 열람 시작 시 호출합니다. 조회수 1로 집계되며 반환된 sessionId로 체류시간을 보고합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "세션 생성 성공"),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서를 찾을 수 없음 — 비공개·삭제 포함 (RESUME_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"이력서를 찾을 수 없습니다"}""")],
                ),
            ],
        ),
    )
    @PostMapping
    fun startViewSession(
        @PathVariable slug: String,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<ViewSessionResponse>> {
        val session =
            startViewSessionUseCase.start(
                StartViewSessionCommand(slug = slug, visitorIp = extractClientIp(servletRequest)),
            )
        return ResponseEntity.ok(ApiResponse.ok(ViewSessionResponse(sessionId = session.id)))
    }

    @Operation(
        summary = "체류시간 보고",
        description = "열람 이탈 시(또는 주기적으로) 전체·섹션별 체류시간을 보고합니다. 재보고 시 마지막 값으로 갱신됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "보고 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "체류시간이 음수 (VALIDATION_FAILED)",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ApiResponse")),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서·세션을 찾을 수 없음",
            content = [
                Content(mediaType = "application/json", schema = Schema(ref = "#/components/schemas/ApiResponse")),
            ],
        ),
    )
    @PutMapping("/{sessionId}")
    fun recordViewDuration(
        @PathVariable slug: String,
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: RecordViewDurationRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        recordViewDurationUseCase.record(
            RecordViewDurationCommand(
                slug = slug,
                sessionId = sessionId,
                totalDurationSec = request.totalDurationSec,
                sectionDwells =
                    request.sectionDwells.map {
                        SectionDwell(section = it.section, dwellSeconds = it.dwellSeconds)
                    },
            ),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun extractClientIp(request: HttpServletRequest): String =
        request
            .getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: request.remoteAddr
}

// ── Requests / Responses ──────────────────────────────────────────────────────

@Schema(description = "체류시간 보고 요청")
data class RecordViewDurationRequest(
    @Schema(description = "전체 체류시간(초)", example = "120")
    val totalDurationSec: Int,
    @Schema(description = "섹션별 체류시간")
    val sectionDwells: List<SectionDwellRequest> = emptyList(),
)

@Schema(description = "섹션 체류시간 항목")
data class SectionDwellRequest(
    @Schema(description = "섹션", example = "CAREER")
    val section: BlockType,
    @Schema(description = "체류시간(초)", example = "45")
    val dwellSeconds: Int,
)

@Schema(description = "열람 세션 생성 응답")
data class ViewSessionResponse(
    @Schema(description = "세션 ID (체류시간 보고에 사용)", example = "7")
    val sessionId: Long,
)
