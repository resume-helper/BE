package com.atomiccv.auth.infrastructure.client

class NaverOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {
    private val response: Map<*, *>
        get() = attributes["response"] as Map<*, *>

    override fun getId(): String = response["id"] as String

    override fun getEmail(): String = response["email"] as String

    override fun getName(): String = response["name"] as String

    override fun getProfileImageUrl(): String? = response["profile_image"] as? String
}
