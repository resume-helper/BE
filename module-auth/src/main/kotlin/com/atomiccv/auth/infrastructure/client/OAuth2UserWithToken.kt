package com.atomiccv.auth.infrastructure.client

import org.springframework.security.oauth2.core.user.OAuth2User

class OAuth2UserWithToken(
    private val delegate: OAuth2User,
    val accessToken: String,
    val refreshToken: String,
) : OAuth2User by delegate
