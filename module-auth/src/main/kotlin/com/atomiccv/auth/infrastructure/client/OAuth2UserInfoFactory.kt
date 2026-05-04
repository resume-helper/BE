package com.atomiccv.auth.infrastructure.client

object OAuth2UserInfoFactory {
    fun of(
        registrationId: String,
        attributes: Map<String, Any>,
    ): OAuth2UserInfo =
        when (registrationId.lowercase()) {
            "google" -> GoogleOAuth2UserInfo(attributes)
            "kakao" -> KakaoOAuth2UserInfo(attributes)
            "naver" -> NaverOAuth2UserInfo(attributes)
            else -> throw IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다: $registrationId")
        }
}
