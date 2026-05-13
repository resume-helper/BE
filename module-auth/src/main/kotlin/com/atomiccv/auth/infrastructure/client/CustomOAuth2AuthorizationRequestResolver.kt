package com.atomiccv.auth.infrastructure.client

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

const val SESSION_KEY_REDIRECT_URI = "OAUTH2_REDIRECT_URI"

class CustomOAuth2AuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository,
) : OAuth2AuthorizationRequestResolver {
    private val delegate =
        DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val authorizationRequest = delegate.resolve(request) ?: return null
        saveRedirectUri(request)
        return authorizationRequest
    }

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String,
    ): OAuth2AuthorizationRequest? {
        val authorizationRequest = delegate.resolve(request, clientRegistrationId) ?: return null
        saveRedirectUri(request)
        return authorizationRequest
    }

    private fun saveRedirectUri(request: HttpServletRequest) {
        val redirectUri = request.getParameter("redirect_uri") ?: return
        request.getSession(true).setAttribute(SESSION_KEY_REDIRECT_URI, redirectUri)
    }
}
