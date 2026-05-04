package com.atomiccv.auth.application.usecase

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import com.atomiccv.auth.domain.repository.UserRepository
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

data class OAuthLoginCommand(
    val provider: SocialProvider,
    val providerUserId: String,
    val email: String,
    val name: String,
    val profileImageUrl: String?,
)

data class TokenResult(
    val accessToken: String,
    val refreshToken: String,
)

@Transactional
class OAuthLoginUseCase(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val jwtPort: JwtPort,
    private val refreshTokenPort: RefreshTokenPort,
) {
    fun login(command: OAuthLoginCommand): TokenResult {
        val user = resolveUser(command)

        val accessToken = jwtPort.generateAccessToken(user.id)
        val refreshToken = UUID.randomUUID().toString()
        refreshTokenPort.save(user.id, refreshToken, Duration.ofDays(7))

        return TokenResult(accessToken, refreshToken)
    }

    private fun resolveUser(command: OAuthLoginCommand): User {
        // 1. 기존 소셜 계정으로 조회 (재방문)
        val existingSocial =
            socialAccountRepository.findByProviderAndProviderUserId(
                command.provider,
                command.providerUserId,
            )
        if (existingSocial != null) {
            return userRepository.findById(existingSocial.userId)
                ?: error("SocialAccount가 참조하는 User가 없습니다: userId=${existingSocial.userId}")
        }

        // 2. 이메일로 기존 User 조회 (타 제공자 연동) or 3. 신규 가입
        val user =
            userRepository.findByEmail(command.email)
                ?: userRepository.save(
                    User(
                        email = command.email,
                        name = command.name,
                        profileImageUrl = command.profileImageUrl,
                    ),
                )
        socialAccountRepository.save(
            SocialAccount(
                userId = user.id,
                provider = command.provider,
                providerUserId = command.providerUserId,
            ),
        )
        return user
    }
}
