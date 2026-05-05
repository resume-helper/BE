package com.atomiccv.auth.application

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.port.TokenBlacklistPort
import com.atomiccv.auth.application.usecase.WithdrawCommand
import com.atomiccv.auth.application.usecase.WithdrawUseCase
import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.shared.common.exception.BusinessException
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.LocalDateTime

class WithdrawUseCaseTest {
    private val userRepository: SocialAccountRepository = mockk()
    private val socialAccountRepository: SocialAccountRepository = mockk()
    private val jwtPort: JwtPort = mockk()
    private val tokenBlacklistPort: TokenBlacklistPort = mockk()
    private val refreshTokenPort: RefreshTokenPort = mockk()

    private val realUserRepository: UserRepository = mockk()

    private val useCase =
        WithdrawUseCase(
            userRepository = realUserRepository,
            socialAccountRepository = socialAccountRepository,
            jwtPort = jwtPort,
            tokenBlacklistPort = tokenBlacklistPort,
            refreshTokenPort = refreshTokenPort,
        )

    private val command = WithdrawCommand(userId = 1L, provider = SocialProvider.GOOGLE, accessToken = "access-token")

    private val activeSocialAccount =
        SocialAccount(id = 1L, userId = 1L, provider = SocialProvider.GOOGLE, providerUserId = "google-123")

    @Test
    fun `마지막 소셜 계정 탈퇴 시 User도 비활성화되고 토큰이 무효화된다`() {
        val user = User(id = 1L, email = "test@example.com", name = "홍길동")
        every { socialAccountRepository.findByUserIdAndProvider(1L, SocialProvider.GOOGLE) } returns activeSocialAccount
        every { socialAccountRepository.save(any()) } answers { firstArg() }
        every { socialAccountRepository.countActiveByUserId(1L) } returns 0
        every { realUserRepository.findById(1L) } returns user
        every { realUserRepository.save(any()) } answers { firstArg() }
        every { jwtPort.validateToken("access-token") } returns true
        every { jwtPort.getRemainingTtl("access-token") } returns Duration.ofHours(1)
        justRun { tokenBlacklistPort.add("access-token", any()) }
        justRun { refreshTokenPort.deleteByUserId(1L) }

        useCase.withdraw(command)

        verify { socialAccountRepository.save(match { !it.isActive && it.deletedAt != null }) }
        verify { realUserRepository.save(match { !it.isActive && it.deletedAt != null }) }
        verify { tokenBlacklistPort.add("access-token", any()) }
        verify { refreshTokenPort.deleteByUserId(1L) }
    }

    @Test
    fun `다른 활성 소셜 계정이 있으면 User는 비활성화되지 않는다`() {
        every { socialAccountRepository.findByUserIdAndProvider(1L, SocialProvider.GOOGLE) } returns activeSocialAccount
        every { socialAccountRepository.save(any()) } answers { firstArg() }
        every { socialAccountRepository.countActiveByUserId(1L) } returns 1

        useCase.withdraw(command)

        verify(exactly = 0) { realUserRepository.save(any()) }
        verify(exactly = 0) { tokenBlacklistPort.add(any(), any()) }
    }

    @Test
    fun `소셜 계정을 찾을 수 없으면 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { socialAccountRepository.findByUserIdAndProvider(1L, SocialProvider.GOOGLE) } returns null

        assertThrows<BusinessException> { useCase.withdraw(command) }
    }

    @Test
    fun `이미 비활성화된 소셜 계정 탈퇴 시 FORBIDDEN 예외가 발생한다`() {
        val inactiveSocial = activeSocialAccount.copy(isActive = false, deletedAt = LocalDateTime.now())
        every { socialAccountRepository.findByUserIdAndProvider(1L, SocialProvider.GOOGLE) } returns inactiveSocial

        assertThrows<BusinessException> { useCase.withdraw(command) }
    }

    @Test
    fun `만료된 Access Token으로 탈퇴 시 토큰 블랙리스트 등록을 건너뛴다`() {
        val user = User(id = 1L, email = "test@example.com", name = "홍길동")
        every { socialAccountRepository.findByUserIdAndProvider(1L, SocialProvider.GOOGLE) } returns activeSocialAccount
        every { socialAccountRepository.save(any()) } answers { firstArg() }
        every { socialAccountRepository.countActiveByUserId(1L) } returns 0
        every { realUserRepository.findById(1L) } returns user
        every { realUserRepository.save(any()) } answers { firstArg() }
        every { jwtPort.validateToken("access-token") } returns false
        justRun { refreshTokenPort.deleteByUserId(1L) }

        useCase.withdraw(command)

        verify(exactly = 0) { tokenBlacklistPort.add(any(), any()) }
        verify { refreshTokenPort.deleteByUserId(1L) }
    }
}
