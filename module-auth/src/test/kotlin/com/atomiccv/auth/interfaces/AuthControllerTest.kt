package com.atomiccv.auth.interfaces

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.TokenBlacklistPort
import com.atomiccv.auth.application.usecase.LogoutUseCase
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.auth.interfaces.rest.AuthController
import com.atomiccv.auth.interfaces.rest.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(AuthController::class)
@Import(GlobalExceptionHandler::class, AuthControllerTest.MockConfig::class)
class AuthControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var tokenRefreshUseCase: TokenRefreshUseCase

    @Autowired
    lateinit var logoutUseCase: LogoutUseCase

    @Autowired
    lateinit var userRepository: UserRepository

    @TestConfiguration
    class MockConfig {
        @Bean
        fun tokenRefreshUseCase(): TokenRefreshUseCase = mockk()

        @Bean
        fun logoutUseCase(): LogoutUseCase = mockk()

        @Bean
        fun userRepository(): UserRepository = mockk()

        @Bean
        fun jwtPort(): JwtPort = mockk(relaxed = true)

        @Bean
        fun tokenBlacklistPort(): TokenBlacklistPort = mockk(relaxed = true)
    }

    @Test
    @WithMockUser
    fun `POST refresh — 유효한 Refresh Token Cookie로 새 Access Token Cookie를 발급한다`() {
        every { tokenRefreshUseCase.refresh("valid-refresh") } returns "new-access"

        mockMvc
            .post("/api/auth/refresh") {
                with(csrf())
                cookie(Cookie("refresh_token", "valid-refresh"))
            }.andExpect {
                status { isOk() }
                cookie { exists("access_token") }
            }
    }

    @Test
    @WithMockUser
    fun `POST refresh — Refresh Token Cookie 없으면 401을 반환한다`() {
        mockMvc
            .post("/api/auth/refresh") {
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    @WithMockUser
    fun `POST logout — Access Token Cookie가 있으면 로그아웃하고 Cookie를 삭제한다`() {
        every { logoutUseCase.logout("my-token") } returns Unit

        mockMvc
            .post("/api/auth/logout") {
                with(csrf())
                cookie(Cookie("access_token", "my-token"))
            }.andExpect {
                status { isOk() }
                cookie { maxAge("access_token", 0) }
                cookie { maxAge("refresh_token", 0) }
            }
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET me — 인증된 사용자의 정보를 반환한다`() {
        val user = User(id = 1L, email = "test@example.com", name = "홍길동")
        every { userRepository.findById(1L) } returns user

        mockMvc.get("/api/auth/me").andExpect {
            status { isOk() }
            jsonPath("$.data.email") { value("test@example.com") }
            jsonPath("$.data.name") { value("홍길동") }
        }
    }
}
