package com.atomiccv.auth.infrastructure.client

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OAuth2AuthenticationSuccessHandler(
    @Value("\${app.frontend-url}") private val frontendUrl: String,
) : SimpleUrlAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val user = authentication.principal as OAuth2UserWithToken
        addCookie(response, "access_token", user.accessToken, Duration.ofHours(1))
        addCookie(response, "refresh_token", user.refreshToken, Duration.ofDays(7), "/api/auth/refresh")
        redirectStrategy.sendRedirect(request, response, frontendUrl)
    }

    private fun addCookie(
        response: HttpServletResponse,
        name: String,
        value: String,
        maxAge: Duration,
        path: String = "/",
    ) {
        val cookie =
            ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(maxAge)
                .sameSite("Lax")
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
