package com.atomiccv.auth.application

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class TokenRefreshUseCaseTest {
    private val refreshTokenPort: RefreshTokenPort = mockk()
    private val jwtPort: JwtPort = mockk()
    private val useCase = TokenRefreshUseCase(refreshTokenPort, jwtPort)

    @Test
    fun `유효한 Refresh Token으로 새 Access Token을 발급한다`() {
        every { refreshTokenPort.findUserIdByToken("valid-refresh") } returns 1L
        every { jwtPort.generateAccessToken(1L) } returns "new-access-token"

        val result = useCase.refresh("valid-refresh")

        assertEquals("new-access-token", result)
    }

    @Test
    fun `존재하지 않는 Refresh Token은 UNAUTHORIZED 예외를 발생시킨다`() {
        every { refreshTokenPort.findUserIdByToken("invalid") } returns null

        val ex = assertThrows<BusinessException> { useCase.refresh("invalid") }
        assertEquals(ErrorCode.UNAUTHORIZED, ex.errorCode)
    }
}
