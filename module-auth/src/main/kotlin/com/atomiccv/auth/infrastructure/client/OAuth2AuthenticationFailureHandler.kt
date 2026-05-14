package com.atomiccv.auth.infrastructure.client

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2AuthenticationFailureHandler(
    @Value("\${app.frontend-url}") private val frontendUrl: String,
    @Value("\${app.allowed-redirect-origins:}") private val allowedRedirectOriginsRaw: String,
) : SimpleUrlAuthenticationFailureHandler() {
    private val allowedRedirectOrigins: List<String> by lazy {
        allowedRedirectOriginsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val base = resolveRedirectUri(request)
        request.session?.removeAttribute(SESSION_KEY_REDIRECT_URI)
        val targetUrl =
            UriComponentsBuilder
                .fromUriString(base)
                .queryParam("error", "true")
                .build()
                .toUriString()
        redirectStrategy.sendRedirect(request, response, targetUrl)
    }

    private fun resolveRedirectUri(request: HttpServletRequest): String {
        val stored = request.session?.getAttribute(SESSION_KEY_REDIRECT_URI) as? String ?: return frontendUrl
        val isAllowed = allowedRedirectOrigins.any { stored.startsWith(it) }
        return if (isAllowed) stored else frontendUrl
    }
}
