package com.atomiccv.auth.infrastructure.client

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

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
        addCookie(response, "access_token", user.accessToken, 3600)
        addCookie(response, "refresh_token", user.refreshToken, 7 * 24 * 3600, "/api/auth/refresh")
        redirectStrategy.sendRedirect(request, response, frontendUrl)
    }

    private fun addCookie(
        response: HttpServletResponse,
        name: String,
        value: String,
        maxAgeSeconds: Int,
        path: String = "/",
    ) {
        val cookie =
            Cookie(name, value).apply {
                isHttpOnly = true
                secure = true
                this.path = path
                maxAge = maxAgeSeconds
            }
        response.addCookie(cookie)
    }
}
