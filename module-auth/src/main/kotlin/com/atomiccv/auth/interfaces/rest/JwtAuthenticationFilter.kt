package com.atomiccv.auth.interfaces.rest

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.TokenBlacklistPort
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtPort: JwtPort,
    private val tokenBlacklistPort: TokenBlacklistPort,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)
        if (token != null && jwtPort.validateToken(token) && !tokenBlacklistPort.isBlacklisted(token)) {
            val userId = jwtPort.extractUserId(token)
            val auth =
                UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                )
            SecurityContextHolder.getContext().authentication = auth
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? =
        request.cookies?.firstOrNull { it.name == "access_token" }?.value
}
