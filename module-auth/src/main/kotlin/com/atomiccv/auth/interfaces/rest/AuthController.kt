package com.atomiccv.auth.interfaces.rest

import com.atomiccv.auth.application.usecase.LogoutUseCase
import com.atomiccv.auth.application.usecase.SocialLoginUseCase
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.auth.application.usecase.WithdrawCommand
import com.atomiccv.auth.application.usecase.WithdrawUseCase
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.auth.interfaces.rest.dto.SocialLoginRequest
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Auth", description = "인증 API — 토큰 갱신, 로그아웃, 내 정보 조회, 회원 탈퇴")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val tokenRefreshUseCase: TokenRefreshUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val withdrawUseCase: WithdrawUseCase,
    private val userRepository: UserRepository,
    private val socialLoginUseCase: SocialLoginUseCase,
) {
    @Operation(
        summary = "Access Token 갱신",
        description = "refresh_token 요청 헤더로 새 access_token 응답 헤더를 발급한다. BFF(Next.js)가 헤더를 통해 토큰을 주고받는다.",
        security = [SecurityRequirement(name = "refresh_token_header")],
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "토큰 갱신 성공 — access_token 응답 헤더 발급"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "refresh_token 헤더 없음 또는 만료 (UNAUTHORIZED / TOKEN_EXPIRED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                )
            ],
        ),
    )
    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val refreshToken =
            request.getHeader("refresh_token")
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        val newAccessToken = tokenRefreshUseCase.refresh(refreshToken)
        response.setHeader("access_token", newAccessToken)
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @Operation(
        summary = "로그아웃",
        description = "access_token 헤더를 Redis Blacklist에 등록하고 Refresh Token을 삭제한다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "access_token 헤더 없음 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                )
            ],
        ),
    )
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        val accessToken =
            request.getHeader("access_token")
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        logoutUseCase.logout(accessToken)
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "즉시 계정을 비활성화하고 30일 후 영구 삭제합니다. 유예기간 내 재로그인 시 복구됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "탈퇴 처리 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "이미 탈퇴 처리된 계정 (FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"이미 탈퇴 처리된 소셜 계정입니다."}""")],
                )
            ],
        ),
    )
    @DeleteMapping("/withdraw")
    fun withdraw(
        request: HttpServletRequest,
        authentication: Authentication,
        @RequestParam provider: SocialProvider,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId =
            authentication.name.toLongOrNull()
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        val accessToken =
            request.getHeader("access_token")
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        withdrawUseCase.withdraw(WithdrawCommand(userId = userId, provider = provider, accessToken = accessToken))
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @Operation(
        summary = "내 정보 조회",
        description = "access_token 헤더 기반으로 현재 로그인한 사용자 정보를 반환한다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = UserResponse::class))],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음 (RESOURCE_NOT_FOUND)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"사용자를 찾을 수 없습니다."}""")],
                )
            ],
        ),
    )
    @GetMapping("/me")
    fun me(authentication: Authentication): ResponseEntity<ApiResponse<UserResponse>> {
        val userId =
            authentication.name.toLongOrNull()
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        val user =
            userRepository.findById(userId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다.")
        return ResponseEntity.ok(ApiResponse.ok(UserResponse(user.id, user.email, user.name, user.profileImageUrl)))
    }

    @Operation(
        summary = "소셜 로그인",
        description =
            "NextAuth.js로부터 받은 소셜 사용자 정보로 JWT를 발급한다. " +
                "access_token, refresh_token 응답 헤더로 내려보내며, BFF(Next.js)가 받아 자신의 HttpOnly 쿠키로 다시 굽는다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공 — access_token, refresh_token 응답 헤더 발급"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "입력값 검증 실패 (VALIDATION_FAILED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"지원하지 않는 provider입니다"}""")],
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "탈퇴 처리된 계정 (FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"탈퇴 처리된 계정입니다."}""")],
                )
            ],
        ),
    )
    @PostMapping("/social-login")
    fun socialLogin(
        @RequestBody request: SocialLoginRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val tokenResult = socialLoginUseCase.login(request.toCommand())
        response.setHeader("access_token", tokenResult.accessToken)
        response.setHeader("refresh_token", tokenResult.refreshToken)
        return ResponseEntity.ok(ApiResponse.ok())
    }
}

@Schema(description = "내 정보 응답")
data class UserResponse(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @Schema(description = "이메일", example = "user@example.com")
    val email: String,
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    @Schema(description = "프로필 이미지 URL", nullable = true, example = "https://example.com/profile.jpg")
    val profileImageUrl: String?,
)
