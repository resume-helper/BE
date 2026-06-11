package com.atomiccv

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(buildInfo())
            .components(buildComponents())
            .addSecurityItem(SecurityRequirement().addList(BEARER_AUTH).addList(ACCESS_TOKEN_COOKIE))

    private fun buildInfo() =
        Info()
            .title("Atomic CV API")
            .version("v1.0")
            .contact(Contact().name("Atomic CV Team"))
            .description(FE_GUIDE)

    private fun buildComponents(): Components {
        val errorResponseSchema =
            Schema<Any>()
                .type("object")
                .addProperty("success", Schema<Boolean>().type("boolean").example(false))
                .addProperty("message", Schema<String>().type("string").example("에러 메시지"))

        return Components()
            .addSecuritySchemes(
                BEARER_AUTH,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Swagger 테스트용 — 로그인 후 발급된 access_token 값을 입력"),
            ).addSecuritySchemes(
                ACCESS_TOKEN_COOKIE,
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.COOKIE)
                    .name("access_token")
                    .description("JWT Access Token (HttpOnly Cookie, 유효기간 1시간)"),
            ).addSecuritySchemes(
                REFRESH_TOKEN_COOKIE,
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.COOKIE)
                    .name("refresh_token")
                    .description("JWT Refresh Token (HttpOnly Cookie, 유효기간 7일, Path=/api/auth/refresh)"),
            ).addSchemas("ErrorResponse", errorResponseSchema)
    }

    companion object {
        const val BEARER_AUTH = "bearerAuth"
        const val ACCESS_TOKEN_COOKIE = "access_token_cookie"
        const val REFRESH_TOKEN_COOKIE = "refresh_token_cookie"

        private val FE_GUIDE =
            """
            ## FE 연동 가이드

            ### 인증 방식 — HttpOnly Cookie

            | 쿠키명 | 유효기간 | 전송 Path | 비고 |
            |--------|----------|-----------|------|
            | `access_token` | **1시간** | 전체 (`/`) | 모든 API 인증 |
            | `refresh_token` | **7일** | `/api/auth/refresh` 전용 | 자동 갱신용 |

            두 쿠키 모두 `HttpOnly` — JS에서 `document.cookie` 접근 불가.

            **모든 API 요청에 필수 설정:**
            ```js
            axios.defaults.withCredentials = true;
            // 또는
            fetch(url, { credentials: 'include' })
            ```

            ### 소셜 로그인 흐름 (NextAuth.js 기반)

            ```
            1. 프론트엔드(NextAuth.js)가 Google/Kakao/Naver OAuth 처리
            2. NextAuth callback에서 사용자 정보 획득
               → provider, providerUserId, email, name, profileImageUrl
            3. POST /api/auth/social-login 으로 전달
            4. 백엔드 응답: access_token / refresh_token 쿠키 발급
            5. GET /api/auth/me 로 유저 정보 확인
            ```

            요청 예시:
            ```json
            POST /api/auth/social-login
            {
              "provider": "GOOGLE",
              "providerUserId": "1234567890",
              "email": "user@example.com",
              "name": "홍길동",
              "profileImageUrl": "https://example.com/profile.jpg"
            }
            ```

            ### 401 처리 흐름

            ```
            API 응답 401
             └─ POST /api/auth/refresh
                  ├─ 성공 → access_token 쿠키 갱신 → 원래 요청 재시도
                  └─ 실패 → 로그인 페이지 이동
            ```

            ### CORS

            - 허용 Origin: 배포 환경별 FE 도메인 (백엔드 환경변수로 관리)
            - `allowCredentials = true` 설정되어 있음

            ---

            ## 에러 응답 포맷

            ```json
            { "success": false, "message": "에러 메시지" }
            ```

            ## 에러 코드 목록

            | HTTP | code | 설명 |
            |------|------|------|
            | 400 | `VALIDATION_FAILED` | 입력값 유효성 검증 실패 (지원하지 않는 provider 포함) |
            | 401 | `UNAUTHORIZED` | 인증 필요 (쿠키 없음) |
            | 401 | `TOKEN_EXPIRED` | Access Token 만료 → `/api/auth/refresh` 호출 |
            | 401 | `INVALID_TOKEN` | 토큰 위변조 또는 형식 오류 |
            | 403 | `FORBIDDEN` | 접근 권한 없음 (탈퇴·정지 계정 포함) |
            | 404 | `RESOURCE_NOT_FOUND` | 리소스 없음 |
            | 409 | `DUPLICATE_EMAIL` | 이미 사용 중인 이메일 |
            | 429 | `RATE_LIMIT_EXCEEDED` | 요청 횟수 초과 |
            | 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |
            | 400 | `BLOCK_DRAFT_TITLE_REQUIRED` | 임시저장 제목 누락 |
            | 403 | `BLOCK_DRAFT_FORBIDDEN` | 임시저장 접근 권한 없음 |
            | 404 | `BLOCK_DRAFT_NOT_FOUND` | 임시저장 없음 또는 만료 |
            """.trimIndent()
    }
}
