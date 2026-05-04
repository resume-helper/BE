package com.atomiccv.auth.infrastructure.client

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error

class NaverOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {
    private val response: Map<*, *>
        get() =
            attributes["response"] as? Map<*, *>
                ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Naver: response 없음", ""))

    override fun getId(): String =
        response["id"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Naver: id 없음", ""))

    override fun getEmail(): String =
        response["email"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Naver: email 없음", ""))

    override fun getName(): String =
        response["name"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Naver: name 없음", ""))

    override fun getProfileImageUrl(): String? = response["profile_image"] as? String
}
