package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.GetPublicResumeQuery
import com.atomiccv.resume.application.usecase.GetPublicResumeUseCase
import com.atomiccv.resume.domain.repository.ResumeDetail
import com.atomiccv.shared.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Public Resume", description = "공개 웹 이력서 열람 API — 비로그인 외부 열람자용")
@RestController
@RequestMapping("/api/public/resumes")
class PublicResumeController(
    private val getPublicResumeUseCase: GetPublicResumeUseCase,
) {
    @Operation(
        summary = "공개 이력서 열람",
        description = "공유 링크의 slug로 공개 웹 이력서를 인증 없이 조회합니다. 비공개·삭제된 이력서는 404를 반환합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
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
    @GetMapping("/{slug}")
    fun getPublicResume(
        @PathVariable slug: String,
    ): ResponseEntity<ApiResponse<PublicResumeResponse>> {
        val detail = getPublicResumeUseCase.getDetail(GetPublicResumeQuery(slug = slug))
        return ResponseEntity.ok(ApiResponse.ok(detail.toPublicResponse()))
    }
}

// ── Responses ─────────────────────────────────────────────────────────────────

@Schema(description = "공개 이력서 열람 응답 — 소유자 전용 필드 제외")
data class PublicResumeResponse(
    @Schema(description = "이력서 ID (피드백 제출 시 사용)", example = "1")
    val id: Long,
    @Schema(description = "이력서 제목", example = "카카오 백엔드 개발자 이력서")
    val title: String,
    @Schema(description = "연결된 블록 목록")
    val blocks: List<ResumeBlockDetailResponse>,
    @Schema(description = "수정 일시", example = "2026-07-01T10:00:00")
    val updatedAt: LocalDateTime,
)

// ── Extension Functions ───────────────────────────────────────────────────────

fun ResumeDetail.toPublicResponse() =
    PublicResumeResponse(
        id = resume.id,
        title = resume.title,
        blocks = blocks.map { it.toResponse() },
        updatedAt = resume.updatedAt,
    )
