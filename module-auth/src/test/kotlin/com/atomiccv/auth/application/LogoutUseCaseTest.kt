package com.atomiccv.auth.application

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.port.TokenBlacklistPort
import com.atomiccv.auth.application.usecase.LogoutUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration

class LogoutUseCaseTest {
    private val jwtPort: JwtPort = mockk()
    private val tokenBlacklistPort: TokenBlacklistPort = mockk()
    private val refreshTokenPort: RefreshTokenPort = mockk()
    private val useCase = LogoutUseCase(jwtPort, tokenBlacklistPort, refreshTokenPort)

    @Test
    fun `로그아웃 시 Access Token이 Blacklist에 등록되고 Refresh Token이 삭제된다`() {
        every { jwtPort.extractUserId("access-token") } returns 1L
        every { jwtPort.getRemainingTtl("access-token") } returns Duration.ofHours(1)
        every { tokenBlacklistPort.add("access-token", any()) } returns Unit
        every { refreshTokenPort.deleteByUserId(1L) } returns Unit

        useCase.logout("access-token")

        verify { tokenBlacklistPort.add("access-token", any()) }
        verify { refreshTokenPort.deleteByUserId(1L) }
    }
}
