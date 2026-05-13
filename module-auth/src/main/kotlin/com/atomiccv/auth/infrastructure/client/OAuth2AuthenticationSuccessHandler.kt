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
    @Value("\${app.cookie-same-site:Lax}") private val cookieSameSite: String,
    @Value("\${app.cookie-domain:}") private val cookieDomain: String,
    @Value("\${app.allowed-redirect-origins:}") private val allowedRedirectOriginsRaw: String,
) : SimpleUrlAuthenticationSuccessHandler() {
    private val allowedRedirectOrigins: List<String> by lazy {
        allowedRedirectOriginsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val user = authentication.principal as OAuth2UserWithToken
        addCookie(response, "access_token", user.accessToken, Duration.ofHours(1))
        addCookie(response, "refresh_token", user.refreshToken, Duration.ofDays(7), "/api/auth/refresh")
        val redirectUri = resolveRedirectUri(request)
        request.session?.removeAttribute(SESSION_KEY_REDIRECT_URI)
        redirectStrategy.sendRedirect(request, response, redirectUri)
    }

    private fun resolveRedirectUri(request: HttpServletRequest): String {
        val stored = request.session?.getAttribute(SESSION_KEY_REDIRECT_URI) as? String ?: return frontendUrl
        val isAllowed = allowedRedirectOrigins.any { stored.startsWith(it) }
        return if (isAllowed) stored else frontendUrl
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
                .sameSite(cookieSameSite)
                .apply { if (cookieDomain.isNotBlank()) domain(cookieDomain) }
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
