package com.atomiccv.auth.application

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.usecase.OAuthLoginCommand
import com.atomiccv.auth.application.usecase.OAuthLoginUseCase
import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import com.atomiccv.auth.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OAuthLoginUseCaseTest {
    private val userRepository: UserRepository = mockk()
    private val socialAccountRepository: SocialAccountRepository = mockk()
    private val jwtPort: JwtPort = mockk()
    private val refreshTokenPort: RefreshTokenPort = mockk()

    private val useCase =
        OAuthLoginUseCase(
            userRepository,
            socialAccountRepository,
            jwtPort,
            refreshTokenPort,
        )

    private val command =
        OAuthLoginCommand(
            provider = SocialProvider.GOOGLE,
            providerUserId = "google-123",
            email = "test@example.com",
            name = "홍길동",
            profileImageUrl = "https://example.com/photo.jpg",
        )

    @Test
    fun `신규 사용자 소셜 로그인 시 User와 SocialAccount가 저장되고 토큰이 반환된다`() {
        val savedUser = User(id = 1L, email = command.email, name = command.name)
        val userSlot = slot<User>()
        val socialSlot = slot<SocialAccount>()

        every { socialAccountRepository.findByProviderAndProviderUserId(any(), any()) } returns null
        every { userRepository.findByEmail(command.email) } returns null
        every { userRepository.save(capture(userSlot)) } returns savedUser
        every { socialAccountRepository.save(capture(socialSlot)) } answers { firstArg() }
        every { jwtPort.generateAccessToken(1L) } returns "access-token"
        every { refreshTokenPort.save(1L, any(), any()) } returns Unit

        val result = useCase.login(command)

        assertEquals("access-token", result.accessToken)
        assertNotNull(result.refreshToken)
        assertEquals(command.email, userSlot.captured.email)
        assertEquals(SocialProvider.GOOGLE, socialSlot.captured.provider)
        assertEquals("google-123", socialSlot.captured.providerUserId)
    }

    @Test
    fun `재방문 사용자는 기존 User를 조회하고 새 토큰을 발급한다`() {
        val existingUser = User(id = 2L, email = command.email, name = command.name)
        val existingSocial =
            SocialAccount(
                id = 1L,
                userId = 2L,
                provider = SocialProvider.GOOGLE,
                providerUserId = "google-123",
            )

        every {
            socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-123")
        } returns existingSocial
        every { userRepository.findById(2L) } returns existingUser
        every { jwtPort.generateAccessToken(2L) } returns "access-token-2"
        every { refreshTokenPort.save(2L, any(), any()) } returns Unit

        val result = useCase.login(command)

        assertEquals("access-token-2", result.accessToken)
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { socialAccountRepository.save(any()) }
    }

    @Test
    fun `동일 이메일로 다른 제공자 로그인 시 기존 User에 SocialAccount가 추가된다`() {
        val existingUser = User(id = 3L, email = command.email, name = command.name)
        val kakaoCommand = command.copy(provider = SocialProvider.KAKAO, providerUserId = "kakao-456")
        val socialSlot = slot<SocialAccount>()

        every {
            socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-456")
        } returns null
        every { userRepository.findByEmail(command.email) } returns existingUser
        every { userRepository.findById(3L) } returns existingUser
        every { socialAccountRepository.save(capture(socialSlot)) } answers { firstArg() }
        every { jwtPort.generateAccessToken(3L) } returns "access-token-3"
        every { refreshTokenPort.save(3L, any(), any()) } returns Unit

        val result = useCase.login(kakaoCommand)

        assertEquals("access-token-3", result.accessToken)
        assertEquals(SocialProvider.KAKAO, socialSlot.captured.provider)
        assertEquals(3L, socialSlot.captured.userId)
        verify(exactly = 0) { userRepository.save(any()) }
    }
}
