package com.atomiccv.auth.application.usecase

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

class TokenRefreshUseCase(
    private val refreshTokenPort: RefreshTokenPort,
    private val jwtPort: JwtPort,
) {
    fun refresh(refreshToken: String): String {
        val userId =
            refreshTokenPort.findUserIdByToken(refreshToken)
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        return jwtPort.generateAccessToken(userId)
    }
}
