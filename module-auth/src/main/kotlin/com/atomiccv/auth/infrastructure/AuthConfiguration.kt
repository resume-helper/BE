package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.port.TokenBlacklistPort
import com.atomiccv.auth.application.usecase.LogoutUseCase
import com.atomiccv.auth.application.usecase.OAuthLoginUseCase
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import com.atomiccv.auth.domain.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 인증 모듈 UseCase를 Spring 빈으로 등록한다.
 * UseCase는 DDD 원칙에 따라 Spring 어노테이션 없이 작성되므로,
 * Infrastructure 레이어에서 @Bean으로 명시 등록한다.
 */
@Configuration
class AuthConfiguration {

    @Bean
    fun oAuthLoginUseCase(
        userRepository: UserRepository,
        socialAccountRepository: SocialAccountRepository,
        jwtPort: JwtPort,
        refreshTokenPort: RefreshTokenPort,
    ): OAuthLoginUseCase =
        OAuthLoginUseCase(
            userRepository = userRepository,
            socialAccountRepository = socialAccountRepository,
            jwtPort = jwtPort,
            refreshTokenPort = refreshTokenPort,
        )

    @Bean
    fun tokenRefreshUseCase(
        refreshTokenPort: RefreshTokenPort,
        jwtPort: JwtPort,
    ): TokenRefreshUseCase =
        TokenRefreshUseCase(
            refreshTokenPort = refreshTokenPort,
            jwtPort = jwtPort,
        )

    @Bean
    fun logoutUseCase(
        jwtPort: JwtPort,
        tokenBlacklistPort: TokenBlacklistPort,
        refreshTokenPort: RefreshTokenPort,
    ): LogoutUseCase =
        LogoutUseCase(
            jwtPort = jwtPort,
            tokenBlacklistPort = tokenBlacklistPort,
            refreshTokenPort = refreshTokenPort,
        )
}
