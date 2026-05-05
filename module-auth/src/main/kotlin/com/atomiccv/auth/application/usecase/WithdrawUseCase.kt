package com.atomiccv.auth.application.usecase

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.port.TokenBlacklistPort
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class WithdrawCommand(
    val userId: Long,
    val provider: SocialProvider,
    val accessToken: String,
)

@Transactional
class WithdrawUseCase(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val jwtPort: JwtPort,
    private val tokenBlacklistPort: TokenBlacklistPort,
    private val refreshTokenPort: RefreshTokenPort,
) {
    fun withdraw(command: WithdrawCommand) {
        val socialAccount =
            socialAccountRepository.findByUserIdAndProvider(command.userId, command.provider)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 소셜 계정을 찾을 수 없습니다.")

        if (!socialAccount.isActive) throw BusinessException(ErrorCode.FORBIDDEN, "이미 탈퇴 처리된 소셜 계정입니다.")

        socialAccountRepository.save(
            socialAccount.copy(isActive = false, deletedAt = LocalDateTime.now()),
        )

        if (socialAccountRepository.countActiveByUserId(command.userId) == 0) {
            val user =
                userRepository.findById(command.userId)
                    ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다.")
            userRepository.save(user.copy(isActive = false, deletedAt = LocalDateTime.now()))
            invalidateTokens(command)
        }
    }

    private fun invalidateTokens(command: WithdrawCommand) {
        if (jwtPort.validateToken(command.accessToken)) {
            val remainingTtl = jwtPort.getRemainingTtl(command.accessToken)
            tokenBlacklistPort.add(command.accessToken, remainingTtl)
        }
        refreshTokenPort.deleteByUserId(command.userId)
    }
}
