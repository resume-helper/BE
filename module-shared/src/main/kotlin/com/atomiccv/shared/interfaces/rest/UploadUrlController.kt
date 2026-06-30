package com.atomiccv.shared.interfaces.rest

import com.atomiccv.shared.application.usecase.GenerateUploadUrlCommand
import com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCase
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
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Upload", description = "S3 업로드 presigned URL 발급 API — resume·block·block-draft 공용")
@RestController
@RequestMapping("/api")
class UploadUrlController(
    private val generateUploadUrlUseCase: GenerateUploadUrlUseCase,
) {
    @Operation(
        summary = "S3 업로드 presigned URL 발급",
        description = "파일 업로드를 위한 S3 presigned PUT URL을 발급합니다. URL 유효 시간은 10분입니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "URL 발급 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "입력값 유효성 검증 실패 (VALIDATION_FAILED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
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
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
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
        val userId =
            authentication.name.toLongOrNull()
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        val result =
            generateUploadUrlUseCase.generate(
                GenerateUploadUrlCommand(userId = userId, fileName = request.fileName),
            )
        return ResponseEntity.ok(
            ApiResponse.ok(UploadUrlResponse(presignedUrl = result.presignedUrl, s3Key = result.s3Key)),
        )
    }
}

@Schema(description = "S3 업로드 URL 발급 요청")
data class GenerateUploadUrlRequest(
    @Schema(description = "업로드할 파일명", example = "resume.pdf")
    @field:NotBlank
    val fileName: String,
)

@Schema(description = "S3 업로드 URL 응답")
data class UploadUrlResponse(
    @Schema(description = "S3 presigned PUT URL", example = "https://s3.amazonaws.com/...")
    val presignedUrl: String,
    @Schema(description = "S3 오브젝트 키", example = "resumes/1/uuid/resume.pdf")
    val s3Key: String,
)
