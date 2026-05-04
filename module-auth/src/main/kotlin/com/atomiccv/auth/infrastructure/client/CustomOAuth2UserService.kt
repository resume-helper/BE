package com.atomiccv.auth.infrastructure.client

import com.atomiccv.auth.application.usecase.OAuthLoginCommand
import com.atomiccv.auth.application.usecase.OAuthLoginUseCase
import com.atomiccv.auth.domain.model.SocialProvider
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val oAuthLoginUseCase: OAuthLoginUseCase,
) : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId
        val userInfo = OAuth2UserInfoFactory.of(registrationId, oAuth2User.attributes)

        val provider = SocialProvider.valueOf(registrationId.uppercase())
        val command =
            OAuthLoginCommand(
                provider = provider,
                providerUserId = userInfo.getId(),
                email = userInfo.getEmail(),
                name = userInfo.getName(),
                profileImageUrl = userInfo.getProfileImageUrl(),
            )

        val tokenResult = oAuthLoginUseCase.login(command)
        return OAuth2UserWithToken(oAuth2User, tokenResult.accessToken, tokenResult.refreshToken)
    }
}
