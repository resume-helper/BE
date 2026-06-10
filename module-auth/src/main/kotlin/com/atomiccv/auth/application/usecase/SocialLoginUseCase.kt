package com.atomiccv.auth.application.usecase

import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class SocialLoginCommand(
    val provider: String,
    val providerUserId: String,
    val email: String,
    val name: String,
)

class SocialLoginUseCase(
    private val oAuthLoginUseCase: OAuthLoginUseCase,
) {
    fun login(command: SocialLoginCommand): TokenResult {
        validate(command)
        // 향후 ID Token 서명 검증 추가 지점
        val providerEnum = parseProvider(command.provider)
        val oAuthCommand =
            OAuthLoginCommand(
                provider = providerEnum,
                providerUserId = command.providerUserId,
                email = command.email,
                name = command.name,
                profileImageUrl = null,
            )
        return oAuthLoginUseCase.login(oAuthCommand)
    }

    private fun validate(command: SocialLoginCommand) {
        if (command.providerUserId.isBlank()) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "providerUserId가 비어있습니다.")
        }
        if (command.email.isBlank()) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "email이 비어있습니다.")
        }
        validateName(command.name)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "name이 비어있습니다.")
        }
    }

    private fun parseProvider(value: String): SocialProvider {
        if (value != value.uppercase()) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "provider는 대문자여야 합니다: $value")
        }
        return runCatching { SocialProvider.valueOf(value) }
            .getOrElse {
                throw BusinessException(ErrorCode.VALIDATION_FAILED, "지원하지 않는 provider입니다: $value")
            }
    }
}
