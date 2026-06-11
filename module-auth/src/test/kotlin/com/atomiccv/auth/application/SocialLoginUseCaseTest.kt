package com.atomiccv.auth.application

import com.atomiccv.auth.application.usecase.OAuthLoginCommand
import com.atomiccv.auth.application.usecase.OAuthLoginUseCase
import com.atomiccv.auth.application.usecase.SocialLoginCommand
import com.atomiccv.auth.application.usecase.SocialLoginUseCase
import com.atomiccv.auth.application.usecase.TokenResult
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SocialLoginUseCaseTest {
    private val oAuthLoginUseCase: OAuthLoginUseCase = mockk()
    private val useCase = SocialLoginUseCase(oAuthLoginUseCase = oAuthLoginUseCase)

    private val validCommand =
        SocialLoginCommand(
            provider = "GOOGLE",
            providerUserId = "google-123",
            email = "test@example.com",
            name = "홍길동",
        )

    @Test
    fun `정상 GOOGLE 요청은 OAuthLoginCommand로 변환되어 위임된다`() {
        val captured = slot<OAuthLoginCommand>()
        every { oAuthLoginUseCase.login(capture(captured)) } returns TokenResult("access", "refresh")

        val result = useCase.login(validCommand)

        assertEquals("access", result.accessToken)
        assertEquals("refresh", result.refreshToken)
        assertEquals(SocialProvider.GOOGLE, captured.captured.provider)
        assertEquals("google-123", captured.captured.providerUserId)
        assertEquals("test@example.com", captured.captured.email)
        assertEquals("홍길동", captured.captured.name)
        assertEquals(null, captured.captured.profileImageUrl)
    }

    @Test
    fun `KAKAO와 NAVER도 동일하게 변환된다`() {
        val captured = slot<OAuthLoginCommand>()
        every { oAuthLoginUseCase.login(capture(captured)) } returns TokenResult("a", "r")

        useCase.login(validCommand.copy(provider = "KAKAO"))
        assertEquals(SocialProvider.KAKAO, captured.captured.provider)

        useCase.login(validCommand.copy(provider = "NAVER"))
        assertEquals(SocialProvider.NAVER, captured.captured.provider)
    }

    @Test
    fun `유효하지 않은 provider 값은 VALIDATION_FAILED를 발생시킨다`() {
        val ex =
            assertThrows<BusinessException> {
                useCase.login(validCommand.copy(provider = "INVALID"))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `소문자 provider는 거부된다`() {
        val ex =
            assertThrows<BusinessException> {
                useCase.login(validCommand.copy(provider = "google"))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `빈 email은 VALIDATION_FAILED를 발생시킨다`() {
        val ex =
            assertThrows<BusinessException> {
                useCase.login(validCommand.copy(email = ""))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `빈 name은 VALIDATION_FAILED를 발생시킨다`() {
        val ex =
            assertThrows<BusinessException> {
                useCase.login(validCommand.copy(name = ""))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `빈 providerUserId는 VALIDATION_FAILED를 발생시킨다`() {
        val ex =
            assertThrows<BusinessException> {
                useCase.login(validCommand.copy(providerUserId = ""))
            }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode)
    }

    @Test
    fun `위임 호출의 예외는 그대로 전파된다`() {
        every { oAuthLoginUseCase.login(any()) } throws BusinessException(ErrorCode.FORBIDDEN, "탈퇴 처리된 계정입니다.")

        val ex = assertThrows<BusinessException> { useCase.login(validCommand) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
