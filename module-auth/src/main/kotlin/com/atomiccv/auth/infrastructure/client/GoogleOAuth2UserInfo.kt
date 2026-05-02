package com.atomiccv.auth.infrastructure.client

class GoogleOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {
    override fun getId(): String = attributes["sub"] as String

    override fun getEmail(): String = attributes["email"] as String

    override fun getName(): String = attributes["name"] as String

    override fun getProfileImageUrl(): String? = attributes["picture"] as? String
}
