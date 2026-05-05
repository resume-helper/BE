package com.atomiccv.auth.application.usecase

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
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
        val existingSocial =
            socialAccountRepository.findByProviderAndProviderUserId(command.provider, command.providerUserId)
        if (existingSocial != null) return resolveBySocialAccount(existingSocial)
        return resolveByEmailOrCreate(command)
    }

    private fun resolveBySocialAccount(existingSocial: SocialAccount): User {
        val user =
            userRepository.findById(existingSocial.userId)
                ?: error("SocialAccount가 참조하는 User가 없습니다: userId=${existingSocial.userId}")
        if (!existingSocial.isActive) {
            if (!existingSocial.isWithinGracePeriod()) {
                throw BusinessException(ErrorCode.FORBIDDEN, "탈퇴 처리된 계정입니다.")
            }
            val restored = socialAccountRepository.save(existingSocial.copy(isActive = true, deletedAt = null))
            return restoreUserIfInactive(restored.userId)
        }
        return user
    }

    private fun resolveByEmailOrCreate(command: OAuthLoginCommand): User {
        val existingUser =
            userRepository.findByEmail(command.email)
                ?: return createNewUser(command)
        val activeUser = restoreUserIfInactive(existingUser.id)
        socialAccountRepository.save(
            SocialAccount(userId = activeUser.id, provider = command.provider, providerUserId = command.providerUserId),
        )
        return activeUser
    }

    private fun createNewUser(command: OAuthLoginCommand): User {
        val user =
            userRepository.save(
                User(email = command.email, name = command.name, profileImageUrl = command.profileImageUrl),
            )
        socialAccountRepository.save(
            SocialAccount(userId = user.id, provider = command.provider, providerUserId = command.providerUserId),
        )
        return user
    }

    private fun restoreUserIfInactive(userId: Long): User {
        val user = userRepository.findById(userId) ?: error("User를 찾을 수 없습니다: userId=$userId")
        if (!user.isActive) {
            if (!user.isWithinGracePeriod()) {
                throw BusinessException(ErrorCode.FORBIDDEN, "탈퇴 처리된 계정입니다.")
            }
            return userRepository.save(user.copy(isActive = true, deletedAt = null))
        }
        return user
    }
}
