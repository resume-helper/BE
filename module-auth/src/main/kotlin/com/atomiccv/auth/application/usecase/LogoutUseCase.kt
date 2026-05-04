package com.atomiccv.auth.application.usecase

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.port.TokenBlacklistPort

class LogoutUseCase(
    private val jwtPort: JwtPort,
    private val tokenBlacklistPort: TokenBlacklistPort,
    private val refreshTokenPort: RefreshTokenPort,
) {
    fun logout(accessToken: String) {
        // 만료·위변조 토큰도 로그아웃은 조용히 성공 (idempotent)
        if (!jwtPort.validateToken(accessToken)) return
        val userId = jwtPort.extractUserId(accessToken)
        val remainingTtl = jwtPort.getRemainingTtl(accessToken)
        tokenBlacklistPort.add(accessToken, remainingTtl)
        refreshTokenPort.deleteByUserId(userId)
    }
}
