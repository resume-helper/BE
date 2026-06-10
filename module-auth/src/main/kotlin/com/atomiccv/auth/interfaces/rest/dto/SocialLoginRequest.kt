package com.atomiccv.auth.interfaces.rest.dto

import com.atomiccv.auth.application.usecase.SocialLoginCommand

data class SocialLoginRequest(
    val provider: String,
    val providerUserId: String,
    val email: String,
    val name: String,
) {
    fun toCommand(): SocialLoginCommand =
        SocialLoginCommand(
            provider = provider,
            providerUserId = providerUserId,
            email = email,
            name = name,
        )
}
