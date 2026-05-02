package com.atomiccv.auth.infrastructure.client

class KakaoOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {
    private val kakaoAccount: Map<*, *>
        get() = attributes["kakao_account"] as Map<*, *>

    private val profile: Map<*, *>
        get() = kakaoAccount["profile"] as Map<*, *>

    override fun getId(): String = attributes["id"].toString()

    override fun getEmail(): String = kakaoAccount["email"] as String

    override fun getName(): String = profile["nickname"] as String

    override fun getProfileImageUrl(): String? = profile["profile_image_url"] as? String
}
