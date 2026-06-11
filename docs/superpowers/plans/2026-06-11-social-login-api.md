# Social Login API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /api/auth/social-login` 엔드포인트를 추가해 NextAuth.js로부터 받은 소셜 사용자 정보로 JWT 쿠키를 발급한다.

**Architecture:** 신규 `SocialLoginUseCase`가 입력 검증과 enum 변환을 담당한 뒤 기존 `OAuthLoginUseCase`에 위임한다. DDD Hexagonal 원칙에 따라 UseCase는 순수 Kotlin이며 `AuthConfiguration`에서 `@Bean`으로 등록한다. Controller는 기존 `AuthController`에 메서드로 추가하고 쿠키 설정은 기존 `refresh()` 패턴을 그대로 따른다.

**Tech Stack:** Kotlin 1.9+, Spring Boot 3.x, Spring Security, MockK, JUnit5, kotlin.test, Detekt

---

## File Structure

**Create (4):**
- `module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/SocialLoginUseCase.kt`
- `module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/dto/SocialLoginRequest.kt`
- `module-auth/src/test/kotlin/com/atomiccv/auth/application/SocialLoginUseCaseTest.kt`
- (Controller 테스트는 기존 `AuthControllerTest.kt`에 메서드 추가)

**Modify (3):**
- `module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/AuthConfiguration.kt` — `@Bean socialLoginUseCase` 추가
- `module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/AuthController.kt` — `socialLogin()` 메서드 + `cookieDomain` 필드 추가
- `module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/SecurityConfig.kt` — `permitAll` 목록에 path 추가
- `module-auth/src/test/kotlin/com/atomiccv/auth/interfaces/AuthControllerTest.kt` — social-login 시나리오 추가

---

## Task 1: SocialLoginCommand + SocialLoginUseCase 신규

**Files:**
- Create: `module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/SocialLoginUseCase.kt`
- Test: `module-auth/src/test/kotlin/com/atomiccv/auth/application/SocialLoginUseCaseTest.kt`

- [ ] **Step 1: Write failing tests**

Create `module-auth/src/test/kotlin/com/atomiccv/auth/application/SocialLoginUseCaseTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.application.SocialLoginUseCaseTest" 2>&1 | tail -20
```

Expected: FAIL — `Unresolved reference: SocialLoginUseCase` / `SocialLoginCommand`

- [ ] **Step 3: Write minimal implementation**

Create `module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/SocialLoginUseCase.kt`:

```kotlin
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
        if (command.name.isBlank()) {
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
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.application.SocialLoginUseCaseTest" 2>&1 | tail -20
```

Expected: PASS — 8 tests passed

- [ ] **Step 5: Detekt 검증**

```bash
./gradlew :module-auth:detekt 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/SocialLoginUseCase.kt \
        module-auth/src/test/kotlin/com/atomiccv/auth/application/SocialLoginUseCaseTest.kt
git commit -m "$(cat <<'EOF'
feat(auth): SocialLoginUseCase 추가

NextAuth.js로부터 받은 소셜 사용자 정보를 검증하고
OAuthLoginUseCase에 위임하는 UseCase 신규 구현.
provider 문자열 → SocialProvider enum 변환, 필수 필드 검증.
향후 ID Token 검증 추가를 위한 확장 지점 명시.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: AuthConfiguration에 @Bean 등록

**Files:**
- Modify: `module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/AuthConfiguration.kt`

- [ ] **Step 1: 기존 @Bean 패턴 옆에 socialLoginUseCase 추가**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/AuthConfiguration.kt`:

import 섹션에 추가:
```kotlin
import com.atomiccv.auth.application.usecase.SocialLoginUseCase
```

`@Configuration class AuthConfiguration {` 블록 안에 `oAuthLoginUseCase` `@Bean` 다음에 추가:

```kotlin
    @Bean
    fun socialLoginUseCase(oAuthLoginUseCase: OAuthLoginUseCase): SocialLoginUseCase =
        SocialLoginUseCase(oAuthLoginUseCase = oAuthLoginUseCase)
```

- [ ] **Step 2: 컴파일 검증**

```bash
./gradlew :module-auth:compileKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 전체 모듈 테스트 통과 확인 (회귀 방지)**

```bash
./gradlew :module-auth:test 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Commit**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/AuthConfiguration.kt
git commit -m "$(cat <<'EOF'
feat(auth): AuthConfiguration에 SocialLoginUseCase Bean 등록

DDD Hexagonal 원칙에 따라 application 레이어 UseCase는
infrastructure 레이어에서 @Bean으로 명시 등록한다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: SocialLoginRequest DTO + AuthController.socialLogin() 추가

**Files:**
- Create: `module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/dto/SocialLoginRequest.kt`
- Modify: `module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/AuthController.kt`
- Modify: `module-auth/src/test/kotlin/com/atomiccv/auth/interfaces/AuthControllerTest.kt`

- [ ] **Step 1: AuthControllerTest에 실패하는 social-login 테스트 추가**

`AuthControllerTest.kt`의 `MockConfig` 블록 안에 추가:

```kotlin
        @Bean
        fun socialLoginUseCase(): com.atomiccv.auth.application.usecase.SocialLoginUseCase = mockk()
```

`AuthControllerTest` 클래스 안에 `@Autowired` 필드 추가:

```kotlin
    @Autowired
    lateinit var socialLoginUseCase: com.atomiccv.auth.application.usecase.SocialLoginUseCase
```

클래스 끝부분에 테스트 4개 추가:

```kotlin
    @Test
    @WithMockUser
    fun `POST social-login — 정상 요청 시 access_token과 refresh_token 쿠키를 발급한다`() {
        every {
            socialLoginUseCase.login(any())
        } returns com.atomiccv.auth.application.usecase.TokenResult("at-1", "rt-1")

        mockMvc
            .post("/api/auth/social-login") {
                with(csrf())
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """{"provider":"GOOGLE","providerUserId":"g-1","email":"a@b.c","name":"홍길동"}"""
            }.andExpect {
                status { isOk() }
                cookie { exists("access_token") }
                cookie { exists("refresh_token") }
                cookie { httpOnly("access_token", true) }
                cookie { path("access_token", "/") }
                cookie { path("refresh_token", "/api/auth/refresh") }
                jsonPath("$.success") { value(true) }
            }
    }

    @Test
    @WithMockUser
    fun `POST social-login — body가 누락되면 400을 반환한다`() {
        mockMvc
            .post("/api/auth/social-login") {
                with(csrf())
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                status { is4xxClientError() }
            }
    }

    @Test
    @WithMockUser
    fun `POST social-login — UseCase가 VALIDATION_FAILED를 던지면 400을 반환한다`() {
        every {
            socialLoginUseCase.login(any())
        } throws com.atomiccv.shared.common.exception.BusinessException(
            com.atomiccv.shared.common.exception.ErrorCode.VALIDATION_FAILED,
            "지원하지 않는 provider입니다: XYZ",
        )

        mockMvc
            .post("/api/auth/social-login") {
                with(csrf())
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """{"provider":"XYZ","providerUserId":"x","email":"a@b.c","name":"n"}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `POST social-login — UseCase가 FORBIDDEN을 던지면 403을 반환한다`() {
        every {
            socialLoginUseCase.login(any())
        } throws com.atomiccv.shared.common.exception.BusinessException(
            com.atomiccv.shared.common.exception.ErrorCode.FORBIDDEN,
            "탈퇴 처리된 계정입니다.",
        )

        mockMvc
            .post("/api/auth/social-login") {
                with(csrf())
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """{"provider":"GOOGLE","providerUserId":"g-1","email":"a@b.c","name":"n"}"""
            }.andExpect {
                status { isForbidden() }
            }
    }
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.interfaces.AuthControllerTest" 2>&1 | tail -20
```

Expected: FAIL — 신규 4개 테스트가 컴파일 실패 또는 404 응답

- [ ] **Step 3: SocialLoginRequest DTO 생성**

Create `module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/dto/SocialLoginRequest.kt`:

```kotlin
package com.atomiccv.auth.interfaces.rest.dto

import com.atomiccv.auth.application.usecase.SocialLoginCommand

data class SocialLoginRequest(
    val provider: String,
    val providerUserId: String,
    val email: String,
    val name: String,
) {
    fun toCommand(): SocialLoginCommand =
        SocialLoginCommand(
            provider = provider,
            providerUserId = providerUserId,
            email = email,
            name = name,
        )
}
```

- [ ] **Step 4: AuthController에 socialLogin() 메서드 추가**

`module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/AuthController.kt`:

import 섹션에 추가:
```kotlin
import com.atomiccv.auth.application.usecase.SocialLoginUseCase
import com.atomiccv.auth.interfaces.rest.dto.SocialLoginRequest
import org.springframework.web.bind.annotation.RequestBody
```

생성자에 의존성 추가 (기존 `private val userRepository: UserRepository,` 다음 줄):
```kotlin
    private val socialLoginUseCase: SocialLoginUseCase,
```

그리고 `cookieSameSite` 옆에 `cookieDomain` 필드 추가:
```kotlin
    @Value("\${app.cookie-domain:}") private val cookieDomain: String,
```

`refresh()` 메서드 바로 위 또는 클래스 마지막에 메서드 추가:

```kotlin
    @Operation(
        summary = "소셜 로그인",
        description = "NextAuth.js로부터 받은 소셜 사용자 정보로 JWT 쿠키를 발급한다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공 — access_token, refresh_token 쿠키 발급"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "입력값 검증 실패 (VALIDATION_FAILED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"지원하지 않는 provider입니다"}""")],
                )
            ],
        ),
        SwaggerApiResponse(
            responseCode = "403",
            description = "탈퇴 처리된 계정 (FORBIDDEN)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ErrorResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"탈퇴 처리된 계정입니다."}""")],
                )
            ],
        ),
    )
    @PostMapping("/social-login")
    fun socialLogin(
        @RequestBody request: SocialLoginRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val tokenResult = socialLoginUseCase.login(request.toCommand())
        addAuthCookie(response, "access_token", tokenResult.accessToken, "/", Duration.ofHours(1))
        addAuthCookie(response, "refresh_token", tokenResult.refreshToken, "/api/auth/refresh", Duration.ofDays(7))
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun addAuthCookie(
        response: HttpServletResponse,
        name: String,
        value: String,
        path: String,
        maxAge: Duration,
    ) {
        val cookie =
            ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(maxAge)
                .sameSite(cookieSameSite)
                .apply { if (cookieDomain.isNotBlank()) domain(cookieDomain) }
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.interfaces.AuthControllerTest" 2>&1 | tail -10
```

Expected: PASS — 신규 4개 포함 모두 통과

- [ ] **Step 6: Detekt 검증**

```bash
./gradlew :module-auth:detekt 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/dto/SocialLoginRequest.kt \
        module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/AuthController.kt \
        module-auth/src/test/kotlin/com/atomiccv/auth/interfaces/AuthControllerTest.kt
git commit -m "$(cat <<'EOF'
feat(auth): POST /api/auth/social-login 엔드포인트 추가

NextAuth.js로부터 받은 소셜 사용자 정보를 받아 JWT 쿠키를 발급한다.
- SocialLoginRequest DTO: provider, providerUserId, email, name
- AuthController.socialLogin(): SocialLoginUseCase 위임 후 2개 쿠키 발급
- cookieDomain 필드 추가 (OAuth2AuthenticationSuccessHandler와 동일 패턴)
- addAuthCookie 헬퍼 메서드로 쿠키 설정 일관화

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: SecurityConfig permitAll 추가

**Files:**
- Modify: `module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/SecurityConfig.kt`

- [ ] **Step 1: requestMatchers 목록에 social-login 추가**

`SecurityConfig.kt`의 `authorizeHttpRequests` 블록에서 기존 `requestMatchers("/oauth2/**", "/login/**", ...)` 목록에 `"/api/auth/social-login"` 추가:

```kotlin
                    .requestMatchers(
                        "/oauth2/**",
                        "/login/**",
                        "/api/auth/social-login",   // ← 추가
                        "/actuator/health",
                        "/actuator/prometheus",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                    ).permitAll()
```

- [ ] **Step 2: 컴파일 검증**

```bash
./gradlew :module-auth:compileKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 전체 module-auth 테스트 통과 (회귀)**

```bash
./gradlew :module-auth:test 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Detekt 검증**

```bash
./gradlew :module-auth:detekt 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/SecurityConfig.kt
git commit -m "$(cat <<'EOF'
chore(auth): /api/auth/social-login permitAll 추가

소셜 로그인 엔드포인트는 인증 자체이므로 Spring Security
authorizeHttpRequests permitAll 목록에 추가한다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: 통합 검증 + PR 준비

**Files:** N/A (전체 회귀 확인)

- [ ] **Step 1: 전체 BE 빌드 검증**

```bash
./gradlew build -x test 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 전체 모듈 테스트 (회귀 방지)**

```bash
./gradlew test --continue 2>&1 | tail -20
```

Expected: module-auth, module-resume, module-shared, module-worklog 모두 PASS.
참고: `app:test`의 `AtomicCvApplicationTests.contextLoads()`는 로컬 환경변수 부재로 실패할 수 있으며 이는 무관한 기존 이슈 (CI는 GitHub Secrets로 통과).

- [ ] **Step 3: 수동 검증 — local app 띄울 수 있다면 curl 호출**

옵션 1 — local app 환경변수 셋업 가능한 경우:
```bash
./gradlew bootRun &
sleep 30
curl -i -X POST http://localhost:8080/api/auth/social-login \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"provider":"GOOGLE","providerUserId":"test-1","email":"manual-test@example.com","name":"테스트"}'
# 기대: 200 + Set-Cookie 2개
```

옵션 2 — 통합 테스트로 대체:
이미 Task 3의 `AuthControllerTest`가 응답 형식·쿠키·상태코드를 검증하므로 수동 curl은 PR 머지 후 dev 환경에서 수행.

- [ ] **Step 4: 브랜치 정리 및 push**

```bash
git log --oneline feature/social-login-api ^dev | head -10
```

확인: 4개 커밋 (Task 1, 2, 3, 4) + 선행 docs 커밋 (총 5개)

push (push 직전 deploy-precheck 토큰 확인):
```bash
ls /Users/leeseonro/Document/BE/.claude/.deploy-token-* 2>/dev/null
# 없거나 30분 경과 → /deploy-precheck 스킬 호출 또는 수동 발급
git push -u origin feature/social-login-api
```

- [ ] **Step 5: PR 생성**

```bash
gh pr create --base dev --title "feat(auth): POST /api/auth/social-login 엔드포인트 추가" --body "$(cat <<'EOF'
## Summary
- NextAuth.js로부터 받은 소셜 사용자 정보로 JWT 쿠키를 발급하는 신규 엔드포인트
- 페이로드: `provider` (대문자), `providerUserId`, `email`, `name`
- 기존 `OAuthLoginUseCase` 재사용 — 사용자 조회/생성/소셜 연동 로직 보존
- DDD Hexagonal 원칙 준수 — UseCase는 순수 Kotlin, `AuthConfiguration`에서 `@Bean` 등록
- 향후 ID Token 검증을 위한 확장 지점 명시 (`SocialLoginUseCase.login()` 내부)

## Spec
`docs/superpowers/specs/2026-06-11-social-login-api-design.md`

## Test Results
| 모듈 | 상태 |
|------|------|
| module-auth | PASSED (신규 12개 포함) |
| module-resume | PASSED |
| module-shared | PASSED |
| module-worklog | PASSED |
| app (contextLoads) | 로컬 env 부재로 실패 — CI에서 GitHub Secrets로 통과 예상 |

## Test Plan
- [x] `SocialLoginUseCaseTest` 8개 통과 (검증, enum 변환, 위임 전파)
- [x] `AuthControllerTest` 신규 4개 통과 (성공, body 누락, 400, 403)
- [x] Detekt 통과
- [ ] CI에서 `AtomicCvApplicationTests.contextLoads()` 정상 동작 확인
- [ ] 배포 후 curl 또는 FE 통합으로 200 + Set-Cookie 2개 응답 확인

## Out of Scope (별도 PR)
- ID Token 서명 검증
- 기존 OAuth2 코드 제거 (`OAuth2AuthenticationSuccessHandler` 등)
- `module-auth/CLAUDE.md` 갱신

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Verification Summary

| 단계 | 검증 명령 |
|---|---|
| UseCase 단위 테스트 | `./gradlew :module-auth:test --tests "com.atomiccv.auth.application.SocialLoginUseCaseTest"` |
| Controller 통합 테스트 | `./gradlew :module-auth:test --tests "com.atomiccv.auth.interfaces.AuthControllerTest"` |
| 전체 모듈 테스트 (회귀) | `./gradlew :module-auth:test` |
| Detekt | `./gradlew :module-auth:detekt` |
| 빌드 | `./gradlew build -x test` |
| 수동 curl | dev 환경 배포 후 `POST /api/auth/social-login` → 200 + 2x Set-Cookie |

## Out of Scope

- ID Token 서명 검증 (별도 PR)
- 기존 OAuth2 Client 코드 제거 (별도 PR)
- `module-auth/CLAUDE.md` 갱신 (별도 PR)
- FE NextAuth.js 통합 (FE팀 작업)
