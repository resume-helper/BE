package com.atomiccv

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.swagger.v3.oas.models.responses.ApiResponse as OasApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses as OasApiResponses

@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(buildInfo())
            .components(buildComponents())
            .addSecurityItem(SecurityRequirement().addList(ACCESS_TOKEN_COOKIE))
            .addTagsItem(Tag().name("OAuth2 소셜 로그인").description("소셜 로그인 시작 — 브라우저를 해당 URL로 이동"))
            .paths(buildOAuth2Paths())

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

    private fun buildOAuth2Paths(): Paths {
        val paths = Paths()
        listOf("google" to "Google", "kakao" to "Kakao", "naver" to "Naver").forEach { (id, name) ->
            paths.addPathItem(
                "/oauth2/authorization/$id",
                PathItem().get(
                    Operation()
                        .summary("$name 소셜 로그인")
                        .description(
                            "브라우저를 이 URL로 이동시킵니다 (`window.location.href = ...`).\n\n" +
                                "로그인 성공 후 프론트엔드 URL로 리다이렉트되며 " +
                                "`access_token`(1h), `refresh_token`(7d) 쿠키가 자동 설정됩니다.",
                        ).addTagsItem("OAuth2 소셜 로그인")
                        .security(emptyList())
                        .responses(
                            OasApiResponses()
                                .addApiResponse("302", OasApiResponse().description("$name 인증 서버로 리다이렉트"))
                                .addApiResponse(
                                    "502",
                                    OasApiResponse().description("OAUTH2_PROVIDER_ERROR — 소셜 로그인 제공자 오류"),
                                ),
                        ),
                ),
            )
        }
        return paths
    }

    companion object {
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

            ### 소셜 로그인 흐름

            ```
            1. window.location.href = '/oauth2/authorization/{google|kakao|naver}'
            2. OAuth2 제공자 로그인/동의
            3. 백엔드 → 프론트엔드 URL로 리다이렉트 (쿠키 자동 설정)
            4. GET /api/auth/me 로 유저 정보 확인
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
            | 400 | `VALIDATION_FAILED` | 입력값 유효성 검증 실패 |
            | 401 | `UNAUTHORIZED` | 인증 필요 (쿠키 없음) |
            | 401 | `TOKEN_EXPIRED` | Access Token 만료 → `/api/auth/refresh` 호출 |
            | 401 | `INVALID_TOKEN` | 토큰 위변조 또는 형식 오류 |
            | 403 | `FORBIDDEN` | 접근 권한 없음 (탈퇴·정지 계정 포함) |
            | 404 | `RESOURCE_NOT_FOUND` | 리소스 없음 |
            | 409 | `DUPLICATE_EMAIL` | 이미 사용 중인 이메일 |
            | 429 | `RATE_LIMIT_EXCEEDED` | 요청 횟수 초과 |
            | 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |
            | 502 | `OAUTH2_PROVIDER_ERROR` | 소셜 로그인 제공자 오류 |
            """.trimIndent()
    }
}
