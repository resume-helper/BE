package com.atomiccv.auth.infrastructure.client

interface OAuth2UserInfo {
    fun getId(): String

    fun getEmail(): String

    fun getName(): String

    fun getProfileImageUrl(): String?
}
