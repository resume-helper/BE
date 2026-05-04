package com.atomiccv.auth.interfaces.rest

import com.atomiccv.auth.application.usecase.LogoutUseCase
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Auth", description = "인증 API — 토큰 갱신, 로그아웃, 내 정보 조회")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val tokenRefreshUseCase: TokenRefreshUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userRepository: UserRepository,
) {
    @Operation(
        summary = "Access Token 갱신",
        description = "refresh_token 쿠키를 사용해 새로운 access_token 쿠키를 발급합니다.",
        security = [SecurityRequirement(name = "refresh_token_cookie")],
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "refresh_token 쿠키 없음 또는 만료",
            content = [Content(schema = Schema(hidden = true))]
        ),
    )
    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val refreshToken =
            request.cookies?.firstOrNull { it.name == "refresh_token" }?.value
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        val newAccessToken = tokenRefreshUseCase.refresh(refreshToken)
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            ResponseCookie
                .from("access_token", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .build()
                .toString(),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @Operation(
        summary = "로그아웃",
        description = "access_token을 Redis Blacklist에 등록하고 Refresh Token을 삭제합니다. 쿠키를 만료시킵니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "access_token 쿠키 없음",
            content = [Content(schema = Schema(hidden = true))]
        ),
    )
    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val accessToken =
            request.cookies?.firstOrNull { it.name == "access_token" }?.value
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        logoutUseCase.logout(accessToken)

        listOf("access_token", "refresh_token").forEach { cookieName ->
            response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie
                    .from(cookieName, "")
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .sameSite("Lax")
                    .build()
                    .toString(),
            )
        }
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @Operation(
        summary = "내 정보 조회",
        description = "access_token 쿠키를 기반으로 현재 로그인한 사용자 정보를 반환합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = UserResponse::class))],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청",
            content = [Content(schema = Schema(hidden = true))]
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = [Content(schema = Schema(hidden = true))]
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
