package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.ReorderBlocksCommand
import com.atomiccv.resume.application.usecase.ReorderBlocksUseCase
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
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "ResumeBlock", description = "이력서 블록 순서 관리 API")
@RestController
@RequestMapping("/api/resumes")
class ResumeBlockController(
    private val reorderBlocksUseCase: ReorderBlocksUseCase,
) {
    @Operation(
        summary = "이력서 블록 순서 변경",
        description = "이력서에 속한 블록의 순서를 변경합니다. 이력서의 모든 블록 ID를 원하는 순서대로 전달해야 합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "순서 변경 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "블록 목록 불일치 또는 중복 (VALIDATION_FAILED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [
                        ExampleObject(
                            value = """{"success":false,"message":"블록 목록이 이력서의 블록 목록과 일치하지 않습니다."}""",
                        ),
                    ],
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
            description = "이력서를 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"이력서를 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @PutMapping("/{resumeId}/blocks/order")
    fun reorderBlocks(
        authentication: Authentication,
        @PathVariable resumeId: Long,
        @Valid @RequestBody request: ReorderBlocksRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        reorderBlocksUseCase.reorder(
            ReorderBlocksCommand(
                resumeId = resumeId,
                userId = userId,
                blockIds = request.blockIds,
            ),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
}

@Schema(description = "블록 순서 변경 요청")
data class ReorderBlocksRequest(
    @Schema(
        description = "변경할 순서대로 나열한 블록 ID 목록 (이력서의 모든 블록 ID를 포함해야 함)",
        example = "[3, 1, 2]",
    )
    val blockIds: List<Long>,
)
