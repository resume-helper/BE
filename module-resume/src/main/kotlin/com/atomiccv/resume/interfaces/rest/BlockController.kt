package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.CreateBlockCommand
import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.GetBlocksQuery
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockCommand
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Block", description = "블록 API — 생성·조회·수정·삭제")
@RestController
@RequestMapping("/api/blocks")
class BlockController(
    private val createBlockUseCase: CreateBlockUseCase,
    private val updateBlockUseCase: UpdateBlockUseCase,
    private val deleteBlockUseCase: DeleteBlockUseCase,
    private val getBlocksUseCase: GetBlocksUseCase,
) {
    @Operation(
        summary = "블록 목록 조회",
        description = "로그인한 사용자의 블록 목록을 반환합니다. `type` 파라미터를 지정하면 해당 타입만 필터링합니다.",
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
    fun getBlocks(
        authentication: Authentication,
        @RequestParam(required = false) type: BlockType?,
    ): ResponseEntity<ApiResponse<List<BlockResponse>>> {
        val userId = resolveUserId(authentication)
        val blocks = getBlocksUseCase.getBlocks(GetBlocksQuery(userId = userId, type = type))
        return ResponseEntity.ok(ApiResponse.ok(blocks.map { it.toResponse() }))
    }

    @Operation(
        summary = "블록 생성",
        description = "새로운 블록을 생성합니다. `contentJson`은 블록 타입에 맞는 JSON 문자열을 그대로 전달합니다.",
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
    fun createBlock(
        authentication: Authentication,
        @Valid @RequestBody request: CreateBlockRequest,
    ): ResponseEntity<ApiResponse<BlockResponse>> {
        val userId = resolveUserId(authentication)
        val block =
            createBlockUseCase.create(
                CreateBlockCommand(
                    userId = userId,
                    type = request.type,
                    title = request.title,
                    contentJson = request.contentJson,
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(block.toResponse()))
    }

    @Operation(
        summary = "블록 수정",
        description = "블록의 제목과 내용을 수정합니다. 본인 소유 블록만 수정 가능합니다.",
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
            description = "본인 소유가 아닌 블록 수정 시도 (FORBIDDEN)",
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
            description = "블록을 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"블록을 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @PutMapping("/{id}")
    fun updateBlock(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBlockRequest,
    ): ResponseEntity<ApiResponse<BlockResponse>> {
        val userId = resolveUserId(authentication)
        val block =
            updateBlockUseCase.update(
                UpdateBlockCommand(
                    blockId = id,
                    userId = userId,
                    title = request.title,
                    contentJson = request.contentJson,
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(block.toResponse()))
    }

    @Operation(
        summary = "블록 삭제",
        description = "블록을 소프트 삭제합니다. 본인 소유 블록만 삭제 가능합니다.",
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
            description = "본인 소유가 아닌 블록 삭제 시도 (FORBIDDEN)",
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
            description = "블록을 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"블록을 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @DeleteMapping("/{id}")
    fun deleteBlock(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockUseCase.delete(blockId = id, userId = userId)
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
}

@Schema(description = "블록 생성 요청")
data class CreateBlockRequest(
    @Schema(description = "블록 타입", example = "CAREER")
    val type: BlockType,
    @Schema(description = "블록 제목 (최대 200자)", example = "카카오 백엔드 개발자")
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @Schema(description = "블록 내용 JSON", example = """{"company":"카카오","startDate":"2024-01"}""")
    @field:NotBlank
    val contentJson: String,
)

@Schema(description = "블록 수정 요청")
data class UpdateBlockRequest(
    @Schema(description = "블록 제목 (최대 200자)", example = "수정된 제목")
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @Schema(description = "블록 내용 JSON", example = """{"company":"카카오","startDate":"2024-01"}""")
    @field:NotBlank
    val contentJson: String,
)

@Schema(description = "블록 응답")
data class BlockResponse(
    @Schema(description = "블록 ID", example = "1")
    val id: Long,
    @Schema(description = "블록 타입", example = "CAREER")
    val type: BlockType,
    @Schema(description = "블록 제목", example = "카카오 백엔드 개발자")
    val title: String,
    @Schema(description = "블록 내용 JSON", example = """{"company":"카카오","startDate":"2024-01"}""")
    val contentJson: String,
    @Schema(description = "생성 일시", example = "2026-05-11T10:00:00")
    val createdAt: LocalDateTime,
    @Schema(description = "수정 일시", example = "2026-05-11T10:00:00")
    val updatedAt: LocalDateTime,
)

fun Block.toResponse() =
    BlockResponse(
        id = id,
        type = type,
        title = title,
        contentJson = contentJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
