package com.atomiccv.auth.infrastructure.client

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error

class KakaoOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {
    private val kakaoAccount: Map<*, *>
        get() =
            attributes["kakao_account"] as? Map<*, *>
                ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Kakao: kakao_account 없음", ""))

    private val profile: Map<*, *>
        get() =
            kakaoAccount["profile"] as? Map<*, *>
                ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Kakao: profile 없음", ""))

    override fun getId(): String = attributes["id"].toString()

    override fun getEmail(): String =
        kakaoAccount["email"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Kakao: email 없음", ""))

    override fun getName(): String =
        profile["nickname"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_response", "Kakao: nickname 없음", ""))

    override fun getProfileImageUrl(): String? = profile["profile_image_url"] as? String
}
