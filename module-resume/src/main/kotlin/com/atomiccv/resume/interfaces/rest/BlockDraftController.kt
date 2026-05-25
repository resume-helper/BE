package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.BlockDraftSummary
import com.atomiccv.resume.application.usecase.CreateBlockDraftCommand
import com.atomiccv.resume.application.usecase.CreateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeCommand
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftCommand
import com.atomiccv.resume.application.usecase.DeleteBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsCommand
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftQuery
import com.atomiccv.resume.application.usecase.GetBlockDraftUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftsQuery
import com.atomiccv.resume.application.usecase.GetBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockDraftCommand
import com.atomiccv.resume.application.usecase.UpdateBlockDraftUseCase
import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "BlockDraft", description = "블록 임시저장 API — 생성·조회·수정·삭제·만료 관리")
@RestController
@RequestMapping("/api/block-drafts")
class BlockDraftController(
    private val createBlockDraftUseCase: CreateBlockDraftUseCase,
    private val updateBlockDraftUseCase: UpdateBlockDraftUseCase,
    private val getBlockDraftsUseCase: GetBlockDraftsUseCase,
    private val getBlockDraftUseCase: GetBlockDraftUseCase,
    private val deleteBlockDraftUseCase: DeleteBlockDraftUseCase,
    private val deleteBlockDraftsUseCase: DeleteBlockDraftsUseCase,
    private val deleteAllBlockDraftsByTypeUseCase: DeleteAllBlockDraftsByTypeUseCase,
) {
    @Operation(
        summary = "임시저장 생성",
        description = "새 블록 임시저장을 생성합니다. 만료 기간은 생성 시각 기준 14일입니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "생성 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "제목 누락 (BLOCK_DRAFT_TITLE_REQUIRED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"제목을 입력해주세요"}""")],
                )
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
                )
            ],
        ),
    )
    @PostMapping
    fun createDraft(
        authentication: Authentication,
        @Valid @RequestBody request: CreateBlockDraftRequest,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft =
            createBlockDraftUseCase.create(
                CreateBlockDraftCommand(
                    userId = userId,
                    blockType = request.blockType,
                    title = request.title,
                    contentJson = request.contentJson.toString(),
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @Operation(
        summary = "임시저장 목록 조회",
        description = "블록 타입별 임시저장 목록을 반환합니다. 만료된 항목은 제외됩니다. currentDraftId를 전달하면 현재 편집 중인 항목이 isActive=true로 표시됩니다.",
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
                )
            ],
        ),
    )
    @GetMapping
    fun getDrafts(
        authentication: Authentication,
        @RequestParam type: BlockType,
        @RequestParam(required = false) currentDraftId: Long?,
    ): ResponseEntity<ApiResponse<List<BlockDraftSummaryResponse>>> {
        val userId = resolveUserId(authentication)
        val summaries =
            getBlockDraftsUseCase.getDrafts(
                GetBlockDraftsQuery(userId = userId, blockType = type, currentDraftId = currentDraftId),
            )
        return ResponseEntity.ok(ApiResponse.ok(summaries.map { it.toResponse() }))
    }

    @Operation(
        summary = "임시저장 단건 조회",
        description = "임시저장 ID로 단건을 조회합니다. 만료되었거나 본인 소유가 아니면 오류를 반환합니다.",
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
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "본인 소유가 아닌 임시저장 접근 (BLOCK_DRAFT_FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"임시저장에 접근 권한이 없습니다"}""")],
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "임시저장 없음 또는 만료 (BLOCK_DRAFT_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"임시저장을 찾을 수 없습니다"}""")],
                )
            ],
        ),
    )
    @GetMapping("/{id}")
    fun getDraft(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft = getBlockDraftUseCase.getDraft(GetBlockDraftQuery(draftId = id, userId = userId))
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @Operation(
        summary = "임시저장 덮어쓰기",
        description = "임시저장 내용을 덮어씁니다. 수정 시 만료일이 현재 시각 기준 14일로 연장됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "수정 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "제목 누락 (BLOCK_DRAFT_TITLE_REQUIRED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"제목을 입력해주세요"}""")],
                )
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
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "본인 소유가 아닌 임시저장 수정 시도 (BLOCK_DRAFT_FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"임시저장에 접근 권한이 없습니다"}""")],
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "임시저장 없음 또는 만료 (BLOCK_DRAFT_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"임시저장을 찾을 수 없습니다"}""")],
                )
            ],
        ),
    )
    @PutMapping("/{id}")
    fun updateDraft(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBlockDraftRequest,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft =
            updateBlockDraftUseCase.update(
                UpdateBlockDraftCommand(
                    draftId = id,
                    userId = userId,
                    title = request.title,
                    contentJson = request.contentJson.toString(),
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @Operation(
        summary = "임시저장 단건 삭제",
        description = "임시저장을 삭제합니다. 본인 소유 임시저장만 삭제 가능합니다.",
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
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "본인 소유가 아닌 임시저장 삭제 시도 (BLOCK_DRAFT_FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"임시저장에 접근 권한이 없습니다"}""")],
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "임시저장 없음 (BLOCK_DRAFT_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"임시저장을 찾을 수 없습니다"}""")],
                )
            ],
        ),
    )
    @DeleteMapping("/{id}")
    fun deleteDraft(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockDraftUseCase.delete(DeleteBlockDraftCommand(draftId = id, userId = userId))
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @Operation(
        summary = "임시저장 선택 삭제",
        description = "여러 임시저장을 한 번에 삭제합니다. 본인 소유 항목만 포함되어야 합니다.",
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
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "타인의 임시저장 포함 (BLOCK_DRAFT_FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"임시저장에 접근 권한이 없습니다"}""")],
                )
            ],
        ),
    )
    @DeleteMapping("/batch")
    fun deleteDrafts(
        authentication: Authentication,
        @Valid @RequestBody request: DeleteBlockDraftsRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockDraftsUseCase.deleteAll(
            DeleteBlockDraftsCommand(draftIds = request.ids, userId = userId),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @Operation(
        summary = "임시저장 타입별 전체 삭제",
        description = "특정 블록 타입의 임시저장을 전부 삭제합니다.",
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
                )
            ],
        ),
    )
    @DeleteMapping
    fun deleteAllByType(
        authentication: Authentication,
        @RequestParam type: BlockType,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteAllBlockDraftsByTypeUseCase.deleteAll(
            DeleteAllBlockDraftsByTypeCommand(userId = userId, blockType = type),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
}

@Schema(description = "임시저장 생성 요청")
data class CreateBlockDraftRequest(
    @Schema(description = "블록 타입", example = "CAREER")
    val blockType: BlockType,
    @Schema(description = "임시저장 제목 (최대 200자)", example = "카카오 백엔드 개발자")
    val title: String,
    @Schema(description = "블록 내용 JSON", example = """{"company":"카카오","period":"2023.03~"}""")
    val contentJson: JsonNode,
)

@Schema(description = "임시저장 덮어쓰기 요청")
data class UpdateBlockDraftRequest(
    @Schema(description = "임시저장 제목 (최대 200자)", example = "수정된 제목")
    val title: String,
    @Schema(description = "블록 내용 JSON", example = """{"company":"네이버","period":"2024.01~"}""")
    val contentJson: JsonNode,
)

@Schema(description = "임시저장 선택 삭제 요청")
data class DeleteBlockDraftsRequest(
    @Schema(description = "삭제할 임시저장 ID 목록 (최소 1개)", example = "[1, 2, 3]")
    @field:Size(min = 1)
    val ids: List<Long>,
)

@Schema(description = "임시저장 상세 응답")
data class BlockDraftResponse(
    @Schema(description = "임시저장 ID", example = "1")
    val id: Long,
    @Schema(description = "블록 타입", example = "CAREER")
    val blockType: BlockType,
    @Schema(description = "임시저장 제목", example = "카카오 백엔드 개발자")
    val title: String,
    @Schema(description = "블록 내용 JSON 문자열", example = """{"company":"카카오"}""")
    val contentJson: String,
    @Schema(description = "만료 일시 (생성/수정 기준 14일 후)", example = "2026-06-05T14:30:00")
    val expiresAt: LocalDateTime,
    @Schema(description = "생성 일시", example = "2026-05-22T14:30:00")
    val createdAt: LocalDateTime,
    @Schema(description = "수정 일시", example = "2026-05-22T14:30:00")
    val updatedAt: LocalDateTime,
)

@Schema(description = "임시저장 목록 항목")
data class BlockDraftSummaryResponse(
    @Schema(description = "임시저장 ID", example = "1")
    val id: Long,
    @Schema(description = "임시저장 제목", example = "카카오 백엔드 개발자")
    val title: String,
    @Schema(description = "수정 일시", example = "2026-05-22T14:30:00")
    val updatedAt: LocalDateTime,
    @Schema(description = "현재 편집 중인 항목 여부 (currentDraftId와 일치하면 true)", example = "false")
    val isActive: Boolean,
)

fun BlockDraft.toResponse() =
    BlockDraftResponse(
        id = id,
        blockType = blockType,
        title = title,
        contentJson = contentJson,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BlockDraftSummary.toResponse() =
    BlockDraftSummaryResponse(
        id = id,
        title = title,
        updatedAt = updatedAt,
        isActive = isActive,
    )
