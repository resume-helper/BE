package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.CreateResumeCommand
import com.atomiccv.resume.application.usecase.CreateResumeUseCase
import com.atomiccv.resume.application.usecase.DeleteResumeUseCase
import com.atomiccv.resume.application.usecase.GenerateUploadUrlCommand
import com.atomiccv.resume.application.usecase.GenerateUploadUrlUseCase
import com.atomiccv.resume.application.usecase.GetResumeQuery
import com.atomiccv.resume.application.usecase.GetResumeUseCase
import com.atomiccv.resume.application.usecase.GetResumesQuery
import com.atomiccv.resume.application.usecase.GetResumesUseCase
import com.atomiccv.resume.application.usecase.ResumeBlockInput
import com.atomiccv.resume.application.usecase.UpdateResumeCommand
import com.atomiccv.resume.application.usecase.UpdateResumeUseCase
import com.atomiccv.resume.application.usecase.UpdateResumeVisibilityCommand
import com.atomiccv.resume.application.usecase.UpdateResumeVisibilityUseCase
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.ResumeBlockDetail
import com.atomiccv.resume.domain.repository.ResumeDetail
import com.atomiccv.resume.domain.repository.SortDirection
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import com.atomiccv.shared.common.response.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Resume", description = "이력서 API — 생성·조회·수정·삭제·공개설정·S3 업로드")
@RestController
@RequestMapping("/api/resumes")
class ResumeController(
    private val createResumeUseCase: CreateResumeUseCase,
    private val updateResumeUseCase: UpdateResumeUseCase,
    private val deleteResumeUseCase: DeleteResumeUseCase,
    private val getResumeUseCase: GetResumeUseCase,
    private val getResumesUseCase: GetResumesUseCase,
    private val updateResumeVisibilityUseCase: UpdateResumeVisibilityUseCase,
    private val generateUploadUrlUseCase: GenerateUploadUrlUseCase,
) {
    @Operation(
        summary = "이력서 생성",
        description = "새 이력서를 생성합니다. 블록 목록을 함께 전달하면 연결됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "생성 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "입력값 유효성 검증 실패 (VALIDATION_FAILED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"입력값이 올바르지 않습니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                ),
            ],
        ),
    )
    @PostMapping
    fun createResume(
        authentication: Authentication,
        @Valid @RequestBody request: CreateResumeRequest,
    ): ResponseEntity<ApiResponse<ResumeListItemResponse>> {
        val userId = resolveUserId(authentication)
        val resume =
            createResumeUseCase.create(
                CreateResumeCommand(
                    userId = userId,
                    title = request.title,
                    type = request.type,
                    pdfS3Key = request.pdfS3Key,
                    blocks = request.blocks.map { ResumeBlockInput(blockId = it.blockId, orderIndex = it.orderIndex) },
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(resume.toListResponse()))
    }

    @Operation(
        summary = "이력서 목록 조회",
        description = "로그인한 사용자의 이력서 목록을 페이징하여 반환합니다. type, title 파라미터로 필터링할 수 있습니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                ),
            ],
        ),
    )
    @GetMapping
    fun getResumes(
        authentication: Authentication,
        params: ResumeListParams,
    ): ResponseEntity<ApiResponse<PageResponse<ResumeListItemResponse>>> {
        val userId = resolveUserId(authentication)
        val page =
            getResumesUseCase.getList(
                GetResumesQuery(
                    userId = userId,
                    type = params.type,
                    titleKeyword = params.title,
                    page = params.page,
                    size = params.size,
                    sortDirection = params.sort,
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page) { it.toListResponse() }))
    }

    @Operation(
        summary = "이력서 상세 조회",
        description = "이력서 상세 정보와 연결된 블록 목록을 반환합니다. PDF가 있는 경우 다운로드 URL도 포함됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "본인 소유가 아닌 이력서 접근 (FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"접근 권한이 없습니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서를 찾을 수 없음 (RESUME_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"이력서를 찾을 수 없습니다"}""")],
                ),
            ],
        ),
    )
    @GetMapping("/{id}")
    fun getResume(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ResumeDetailResponse>> {
        val userId = resolveUserId(authentication)
        val detail = getResumeUseCase.getDetail(GetResumeQuery(resumeId = id, userId = userId))
        val downloadUrl = detail.resume.pdfS3Key?.let { getResumeUseCase.getPresignedDownloadUrl(it) }
        return ResponseEntity.ok(ApiResponse.ok(detail.toDetailResponse(downloadUrl)))
    }

    @Operation(
        summary = "이력서 수정",
        description = "이력서 제목과 연결 블록을 수정합니다. 본인 소유 이력서만 수정 가능합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "수정 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "입력값 유효성 검증 실패 (VALIDATION_FAILED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"입력값이 올바르지 않습니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "본인 소유가 아닌 이력서 수정 시도 (FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"접근 권한이 없습니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서를 찾을 수 없음 (RESUME_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"이력서를 찾을 수 없습니다"}""")],
                ),
            ],
        ),
    )
    @PutMapping("/{id}")
    fun updateResume(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateResumeRequest,
    ): ResponseEntity<ApiResponse<ResumeListItemResponse>> {
        val userId = resolveUserId(authentication)
        val resume =
            updateResumeUseCase.update(
                UpdateResumeCommand(
                    resumeId = id,
                    userId = userId,
                    title = request.title,
                    blocks = request.blocks.map { ResumeBlockInput(blockId = it.blockId, orderIndex = it.orderIndex) },
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(resume.toListResponse()))
    }

    @Operation(
        summary = "이력서 삭제",
        description = "이력서를 소프트 삭제합니다. 본인 소유 이력서만 삭제 가능합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "삭제 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "본인 소유가 아닌 이력서 삭제 시도 (FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"접근 권한이 없습니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서를 찾을 수 없음 (RESUME_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"이력서를 찾을 수 없습니다"}""")],
                ),
            ],
        ),
    )
    @DeleteMapping("/{id}")
    fun deleteResume(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteResumeUseCase.delete(resumeId = id, userId = userId)
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @Operation(
        summary = "이력서 공개/비공개 토글",
        description = "이력서의 공개 여부를 변경합니다. 본인 소유 이력서만 변경 가능합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "변경 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "본인 소유가 아닌 이력서 수정 시도 (FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"접근 권한이 없습니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "이력서를 찾을 수 없음 (RESUME_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"이력서를 찾을 수 없습니다"}""")],
                ),
            ],
        ),
    )
    @PatchMapping("/{id}/visibility")
    fun updateVisibility(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestBody request: UpdateVisibilityRequest,
    ): ResponseEntity<ApiResponse<ResumeListItemResponse>> {
        val userId = resolveUserId(authentication)
        val resume =
            updateResumeVisibilityUseCase.update(
                UpdateResumeVisibilityCommand(
                    resumeId = id,
                    userId = userId,
                    isPublic = request.isPublic,
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(resume.toListResponse()))
    }

    @Operation(
        summary = "S3 업로드 presigned URL 발급",
        description = "PDF 파일 업로드를 위한 S3 presigned PUT URL을 발급합니다. URL 유효 시간은 10분입니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "URL 발급 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "입력값 유효성 검증 실패 (VALIDATION_FAILED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"입력값이 올바르지 않습니다"}""")],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                ),
            ],
        ),
    )
    @PostMapping("/upload-url")
    fun generateUploadUrl(
        authentication: Authentication,
        @Valid @RequestBody request: GenerateUploadUrlRequest,
    ): ResponseEntity<ApiResponse<UploadUrlResponse>> {
        val userId = resolveUserId(authentication)
        val result =
            generateUploadUrlUseCase.generate(
                GenerateUploadUrlCommand(userId = userId, fileName = request.fileName),
            )
        return ResponseEntity.ok(
            ApiResponse.ok(UploadUrlResponse(presignedUrl = result.presignedUrl, s3Key = result.s3Key))
        )
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
}

// ── Query Params ──────────────────────────────────────────────────────────────

data class ResumeListParams(
    val page: Int = 1,
    val size: Int = 10,
    val sort: SortDirection = SortDirection.NEWEST,
    val type: ResumeType? = null,
    val title: String? = null,
)

// ── Requests ──────────────────────────────────────────────────────────────────

@Schema(description = "이력서 생성 요청")
data class CreateResumeRequest(
    @Schema(description = "이력서 제목 (최대 200자)", example = "카카오 백엔드 개발자 이력서")
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @Schema(description = "이력서 타입", example = "PDF")
    val type: ResumeType?,
    @Schema(description = "PDF S3 키 (PDF 타입일 때 upload-url로 발급받은 s3Key)", example = "resumes/1/uuid/resume.pdf")
    val pdfS3Key: String? = null,
    @Schema(description = "연결할 블록 목록")
    @field:Valid
    val blocks: List<BlockInputRequest> = emptyList(),
)

@Schema(description = "이력서 수정 요청")
data class UpdateResumeRequest(
    @Schema(description = "이력서 제목 (최대 200자)", example = "수정된 이력서 제목")
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @Schema(description = "연결할 블록 목록")
    @field:Valid
    val blocks: List<BlockInputRequest> = emptyList(),
)

@Schema(description = "블록 입력 항목")
data class BlockInputRequest(
    @Schema(description = "블록 ID", example = "1")
    val blockId: Long,
    @Schema(description = "블록 순서 인덱스", example = "0")
    val orderIndex: Int,
)

@Schema(description = "공개/비공개 설정 요청")
data class UpdateVisibilityRequest(
    @Schema(description = "공개 여부", example = "true")
    val isPublic: Boolean,
)

@Schema(description = "S3 업로드 URL 발급 요청")
data class GenerateUploadUrlRequest(
    @Schema(description = "업로드할 파일명", example = "resume.pdf")
    @field:NotBlank
    val fileName: String,
)

// ── Responses ─────────────────────────────────────────────────────────────────

@Schema(description = "이력서 목록 항목")
data class ResumeListItemResponse(
    @Schema(description = "이력서 ID", example = "1")
    val id: Long,
    @Schema(description = "이력서 제목", example = "카카오 백엔드 개발자 이력서")
    val title: String,
    @Schema(description = "이력서 타입", example = "GENERAL")
    val type: ResumeType?,
    @Schema(description = "공개 여부", example = "false")
    val isPublic: Boolean,
    @Schema(description = "생성 일시", example = "2026-05-11T10:00:00")
    val createdAt: LocalDateTime,
    @Schema(description = "수정 일시", example = "2026-05-11T10:00:00")
    val updatedAt: LocalDateTime,
)

@Suppress("LongParameterList")
@Schema(description = "이력서 상세 응답")
data class ResumeDetailResponse(
    @Schema(description = "이력서 ID", example = "1")
    val id: Long,
    @Schema(description = "이력서 제목", example = "카카오 백엔드 개발자 이력서")
    val title: String,
    @Schema(description = "이력서 타입", example = "GENERAL")
    val type: ResumeType?,
    @Schema(description = "이력서 슬러그 (공개 URL용)", example = "abc123")
    val slug: String?,
    @Schema(description = "공개 여부", example = "false")
    val isPublic: Boolean,
    @Schema(description = "PDF S3 키", example = "resumes/1/uuid/resume.pdf")
    val pdfS3Key: String?,
    @Schema(description = "PDF 다운로드 presigned URL (유효 15분)", example = "https://s3.amazonaws.com/...")
    val pdfDownloadUrl: String?,
    @Schema(description = "연결된 블록 목록")
    val blocks: List<ResumeBlockDetailResponse>,
    @Schema(description = "생성 일시", example = "2026-05-11T10:00:00")
    val createdAt: LocalDateTime,
    @Schema(description = "수정 일시", example = "2026-05-11T10:00:00")
    val updatedAt: LocalDateTime,
)

@Schema(description = "이력서 블록 상세")
data class ResumeBlockDetailResponse(
    @Schema(description = "블록 ID", example = "1")
    val blockId: Long,
    @Schema(description = "블록 순서 인덱스", example = "0")
    val orderIndex: Int,
    @Schema(description = "블록 제목", example = "카카오 백엔드 개발자")
    val title: String,
    @Schema(description = "블록 타입", example = "CAREER")
    val type: BlockType,
    @Schema(description = "블록 내용 JSON 문자열", example = "{}")
    val contentJson: String,
)

@Schema(description = "S3 업로드 URL 응답")
data class UploadUrlResponse(
    @Schema(description = "S3 presigned PUT URL", example = "https://s3.amazonaws.com/...")
    val presignedUrl: String,
    @Schema(description = "S3 오브젝트 키", example = "resumes/1/uuid/resume.pdf")
    val s3Key: String,
)

// ── Extension Functions ───────────────────────────────────────────────────────

fun Resume.toListResponse() =
    ResumeListItemResponse(
        id = id,
        title = title,
        type = type,
        isPublic = isPublic,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ResumeDetail.toDetailResponse(pdfDownloadUrl: String?) =
    ResumeDetailResponse(
        id = resume.id,
        title = resume.title,
        type = resume.type,
        slug = resume.slug,
        isPublic = resume.isPublic,
        pdfS3Key = resume.pdfS3Key,
        pdfDownloadUrl = pdfDownloadUrl,
        blocks = blocks.map { it.toResponse() },
        createdAt = resume.createdAt,
        updatedAt = resume.updatedAt,
    )

fun ResumeBlockDetail.toResponse() =
    ResumeBlockDetailResponse(
        blockId = blockId,
        orderIndex = orderIndex,
        title = title,
        type = type,
        contentJson = contentJson,
    )
