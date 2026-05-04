package com.atomiccv.auth.infrastructure.client

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error

class GoogleOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {
    override fun getId(): String =
        attributes["sub"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Google: sub 없음", ""))

    override fun getEmail(): String =
        attributes["email"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Google: email 없음", ""))

    override fun getName(): String =
        attributes["name"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Google: name 없음", ""))

    override fun getProfileImageUrl(): String? = attributes["picture"] as? String
}
