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
            .addSecurityItem(SecurityRequirement().addList(ACCESS_TOKEN_HEADER))

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
                ACCESS_TOKEN_HEADER,
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.HEADER)
                    .name("access_token")
                    .description("JWT Access Token — 요청 헤더 access_token (유효기간 1시간)"),
            ).addSecuritySchemes(
                REFRESH_TOKEN_HEADER,
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.HEADER)
                    .name("refresh_token")
                    .description("JWT Refresh Token — 요청 헤더 refresh_token (유효기간 7일, /api/auth/refresh 전용)"),
            ).addSchemas("ErrorResponse", errorResponseSchema)
    }

    companion object {
        const val ACCESS_TOKEN_HEADER = "access_token_header"
        const val REFRESH_TOKEN_HEADER = "refresh_token_header"

        private val FE_GUIDE =
            """
            ## FE 연동 가이드

            ### 인증 방식 — 커스텀 헤더

            | 헤더명 | 유효기간 | 사용 위치 |
            |--------|----------|-----------|
            | `access_token` | **1시간** | 모든 보호 API 요청 |
            | `refresh_token` | **7일** | `POST /api/auth/refresh` 호출 시 |

            토큰은 BE 응답 헤더로 내려가며, BFF(Next.js) 가 받아 자신의 HttpOnly 쿠키로 다시 굽는다.
            브라우저는 BE 도메인 쿠키를 직접 보유하지 않는다.

            ### 인증 흐름

            ```
            [로그인]
            POST /api/auth/social-login
              → 응답 헤더: access_token, refresh_token
              → BFF 가 자기 쿠키로 다시 굽기

            [보호 API 호출]
            요청 헤더: access_token: <JWT>

            [Access Token 만료 시 (401)]
            POST /api/auth/refresh
              요청 헤더: refresh_token: <JWT>
              → 응답 헤더: access_token (새 JWT)
              → 원래 요청 재시도
            ```

            ### 401 처리 흐름

            ```
            API 응답 401
             └─ POST /api/auth/refresh (refresh_token 헤더)
                  ├─ 성공 → 새 access_token 헤더 수신 → 원 요청 재시도
                  └─ 실패 → 로그인 페이지 이동
            ```

            ### CORS

            - 허용 Origin: 배포 환경별 BFF 도메인 (백엔드 환경변수로 관리)
            - 커스텀 헤더(`access_token`, `refresh_token`)는 `Access-Control-Expose-Headers` 에 노출

            ---

            ## 에러 응답 포맷

            ```json
            { "success": false, "message": "에러 메시지" }
            ```

            ## 에러 코드 목록

            | HTTP | code | 설명 |
            |------|------|------|
            | 400 | `VALIDATION_FAILED` | 입력값 유효성 검증 실패 |
            | 401 | `UNAUTHORIZED` | 인증 필요 (헤더 없음) |
            | 401 | `TOKEN_EXPIRED` | Access Token 만료 → `/api/auth/refresh` 호출 |
            | 401 | `INVALID_TOKEN` | 토큰 위변조 또는 형식 오류 |
            | 403 | `FORBIDDEN` | 접근 권한 없음 (탈퇴·정지 계정 포함) |
            | 404 | `RESOURCE_NOT_FOUND` | 리소스 없음 |
            | 409 | `DUPLICATE_EMAIL` | 이미 사용 중인 이메일 |
            | 429 | `RATE_LIMIT_EXCEEDED` | 요청 횟수 초과 |
            | 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |
            | 502 | `OAUTH2_PROVIDER_ERROR` | 소셜 로그인 제공자 오류 |
            | 400 | `BLOCK_DRAFT_TITLE_REQUIRED` | 임시저장 제목 누락 |
            | 403 | `BLOCK_DRAFT_FORBIDDEN` | 임시저장 접근 권한 없음 |
            | 404 | `BLOCK_DRAFT_NOT_FOUND` | 임시저장 없음 또는 만료 |
            """.trimIndent()
    }
}
