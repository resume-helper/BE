package com.atomiccv.auth.interfaces.rest

import com.atomiccv.auth.application.usecase.LogoutUseCase
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val tokenRefreshUseCase: TokenRefreshUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userRepository: UserRepository,
) {
    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val refreshToken =
            request.cookies?.firstOrNull { it.name == "refresh_token" }?.value
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        val newAccessToken = tokenRefreshUseCase.refresh(refreshToken)
        response.addCookie(
            Cookie("access_token", newAccessToken).apply {
                isHttpOnly = true
                secure = true
                path = "/"
                maxAge = 3600
            },
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

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
            response.addCookie(
                Cookie(cookieName, "").apply {
                    isHttpOnly = true
                    secure = true
                    path = "/"
                    maxAge = 0
                },
            )
        }
        return ResponseEntity.ok(ApiResponse.ok())
    }

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

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val profileImageUrl: String?,
)
