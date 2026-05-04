# module-auth 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Google/Kakao/Naver 소셜 로그인, JWT HttpOnly Cookie 발급, Redis 기반 Refresh Token·Blacklist 관리를 Hexagonal Architecture로 구현한다.

**Architecture:** Hexagonal Architecture (DDD). domain은 순수 Kotlin, application은 Port 인터페이스만 참조, infrastructure가 Port를 구현. JPA 엔티티는 infrastructure 레이어에만 존재하며 domain 모델과 분리된다.

**Tech Stack:** Kotlin, Spring Boot 3.x, Spring Security OAuth2 Client, jjwt 0.12.6, Spring Data Redis, JPA/Hibernate, MockK, H2 (테스트)

---

## 파일 맵

| 경로 | 역할 | 신규/수정 |
|------|------|----------|
| `module-auth/build.gradle.kts` | MockK 의존성 추가 | 수정 |
| `module-shared/.../ErrorCode.kt` | TOKEN_EXPIRED, INVALID_TOKEN, OAUTH2_PROVIDER_ERROR 추가 | 수정 |
| `domain/model/User.kt` | isActive 기본값 true로 수정, password 필드 제거 | 수정 |
| `domain/model/SocialAccount.kt` | 소셜 계정 연결 도메인 모델 | 신규 |
| `domain/model/SocialProvider.kt` | 기존 유지 | 없음 |
| `domain/model/UserRole.kt` | 기존 유지 | 없음 |
| `domain/repository/UserRepository.kt` | 기존 유지 | 없음 |
| `domain/repository/SocialAccountRepository.kt` | 소셜 계정 조회/저장 인터페이스 | 신규 |
| `application/port/JwtPort.kt` | JWT 발급/검증 포트 인터페이스 | 신규 |
| `application/port/RefreshTokenPort.kt` | Refresh Token 저장/조회/삭제 포트 | 신규 |
| `application/port/TokenBlacklistPort.kt` | Blacklist 등록/조회 포트 | 신규 |
| `application/usecase/OAuthLoginUseCase.kt` | 소셜 로그인 유스케이스 | 신규 |
| `application/usecase/TokenRefreshUseCase.kt` | 토큰 재발급 유스케이스 | 신규 |
| `application/usecase/LogoutUseCase.kt` | 로그아웃 유스케이스 | 신규 |
| `infrastructure/client/JwtProvider.kt` | JwtPort 구현체 | 신규 |
| `infrastructure/client/RefreshTokenRedisAdapter.kt` | RefreshTokenPort 구현체 | 신규 |
| `infrastructure/client/TokenBlacklistRedisAdapter.kt` | TokenBlacklistPort 구현체 | 신규 |
| `infrastructure/client/OAuth2UserInfo.kt` | OAuth2 사용자 정보 인터페이스 | 신규 |
| `infrastructure/client/GoogleOAuth2UserInfo.kt` | Google 응답 파싱 | 신규 |
| `infrastructure/client/KakaoOAuth2UserInfo.kt` | Kakao 응답 파싱 | 신규 |
| `infrastructure/client/NaverOAuth2UserInfo.kt` | Naver 응답 파싱 | 신규 |
| `infrastructure/client/OAuth2UserInfoFactory.kt` | provider → UserInfo 팩토리 | 신규 |
| `infrastructure/client/CustomOAuth2UserService.kt` | Spring OAuth2 UserService 구현 | 신규 |
| `infrastructure/client/OAuth2AuthenticationSuccessHandler.kt` | 로그인 성공 처리, 토큰 Cookie 설정 | 신규 |
| `infrastructure/persistence/UserJpaEntity.kt` | User JPA 엔티티 | 신규 |
| `infrastructure/persistence/SocialAccountJpaEntity.kt` | SocialAccount JPA 엔티티 | 신규 |
| `infrastructure/persistence/UserJpaRepository.kt` | Spring Data JPA 인터페이스 | 신규 |
| `infrastructure/persistence/SocialAccountJpaRepository.kt` | Spring Data JPA 인터페이스 | 신규 |
| `infrastructure/persistence/UserRepositoryImpl.kt` | UserRepository 구현체 | 신규 |
| `infrastructure/persistence/SocialAccountRepositoryImpl.kt` | SocialAccountRepository 구현체 | 신규 |
| `infrastructure/SecurityConfig.kt` | Spring Security 설정 | 신규 |
| `interfaces/rest/AuthController.kt` | /api/auth/{refresh,logout,me} | 신규 |
| `interfaces/rest/JwtAuthenticationFilter.kt` | 요청마다 Access Token 검증 | 신규 |
| `interfaces/rest/GlobalExceptionHandler.kt` | @RestControllerAdvice | 신규 |

> 이하 경로는 모두 `module-auth/src/main/kotlin/com/atomiccv/auth/` 기준.  
> 테스트는 `module-auth/src/test/kotlin/com/atomiccv/auth/` 기준.

---

## Task 1: 기반 설정 — 의존성, 도메인 보정, ErrorCode

**Files:**
- Modify: `module-auth/build.gradle.kts`
- Modify: `module-shared/src/main/kotlin/com/atomiccv/shared/common/exception/ErrorCode.kt`
- Modify: `module-auth/src/main/kotlin/com/atomiccv/auth/domain/model/User.kt`
- Create: `module-auth/src/main/kotlin/com/atomiccv/auth/domain/model/SocialAccount.kt`
- Create: `module-auth/src/main/kotlin/com/atomiccv/auth/domain/repository/SocialAccountRepository.kt`

- [ ] **Step 1: MockK, H2 의존성 추가**

`module-auth/build.gradle.kts`의 `dependencies { }` 블록에 추가:

```kotlin
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("com.h2database:h2")
```

- [ ] **Step 2: ErrorCode에 인증 에러코드 추가**

`module-shared/src/main/kotlin/com/atomiccv/shared/common/exception/ErrorCode.kt`:

```kotlin
enum class ErrorCode(
    val httpStatus: Int,
    val code: String,
    val defaultMessage: String
) {
    VALIDATION_FAILED(400, "VALIDATION_FAILED", "입력값 유효성 검증 실패"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
    TOKEN_EXPIRED(401, "TOKEN_EXPIRED", "액세스 토큰이 만료되었습니다"),
    INVALID_TOKEN(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다"),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다"),
    RATE_LIMIT_EXCEEDED(429, "RATE_LIMIT_EXCEEDED", "요청 횟수를 초과했습니다"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),
    OAUTH2_PROVIDER_ERROR(502, "OAUTH2_PROVIDER_ERROR", "소셜 로그인 제공자 오류가 발생했습니다"),
}
```

- [ ] **Step 3: User 도메인 모델 보정**

`module-auth/src/main/kotlin/com/atomiccv/auth/domain/model/User.kt`:

```kotlin
package com.atomiccv.auth.domain.model

import java.time.LocalDateTime

data class User(
    val id: Long = 0,
    val email: String,
    val name: String,
    val profileImageUrl: String? = null,
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
```

> 변경: `password` 필드 제거 (소셜 로그인 전용), `isActive` 기본값 `true`, `deletedAt` 제거.

- [ ] **Step 4: SocialAccount 도메인 모델 생성**

`module-auth/src/main/kotlin/com/atomiccv/auth/domain/model/SocialAccount.kt`:

```kotlin
package com.atomiccv.auth.domain.model

import java.time.LocalDateTime

data class SocialAccount(
    val id: Long = 0,
    val userId: Long,
    val provider: SocialProvider,
    val providerUserId: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
```

- [ ] **Step 5: SocialAccountRepository 인터페이스 생성**

`module-auth/src/main/kotlin/com/atomiccv/auth/domain/repository/SocialAccountRepository.kt`:

```kotlin
package com.atomiccv.auth.domain.repository

import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider

interface SocialAccountRepository {
    fun save(socialAccount: SocialAccount): SocialAccount
    fun findByProviderAndProviderUserId(provider: SocialProvider, providerUserId: String): SocialAccount?
    fun findAllByUserId(userId: Long): List<SocialAccount>
}
```

- [ ] **Step 6: 빌드 확인**

```bash
./gradlew :module-auth:compileKotlin :module-shared:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add module-auth/build.gradle.kts \
        module-shared/src/main/kotlin/com/atomiccv/shared/common/exception/ErrorCode.kt \
        module-auth/src/main/kotlin/com/atomiccv/auth/domain/
git commit -m "feat(auth): 도메인 모델 보정 및 기반 설정 (SocialAccount, ErrorCode, MockK)"
```

---

## Task 2: Port 인터페이스 정의

**Files:**
- Create: `application/port/JwtPort.kt`
- Create: `application/port/RefreshTokenPort.kt`
- Create: `application/port/TokenBlacklistPort.kt`

- [ ] **Step 1: JwtPort 생성**

`module-auth/src/main/kotlin/com/atomiccv/auth/application/port/JwtPort.kt`:

```kotlin
package com.atomiccv.auth.application.port

import java.time.Duration
import java.util.Date

interface JwtPort {
    fun generateAccessToken(userId: Long): String
    fun validateToken(token: String): Boolean
    fun extractUserId(token: String): Long
    fun getExpiration(token: String): Date
    fun getRemainingTtl(token: String): Duration
}
```

- [ ] **Step 2: RefreshTokenPort 생성**

`module-auth/src/main/kotlin/com/atomiccv/auth/application/port/RefreshTokenPort.kt`:

```kotlin
package com.atomiccv.auth.application.port

import java.time.Duration

interface RefreshTokenPort {
    fun save(userId: Long, token: String, ttl: Duration)
    fun findUserIdByToken(token: String): Long?
    fun deleteByUserId(userId: Long)
}
```

- [ ] **Step 3: TokenBlacklistPort 생성**

`module-auth/src/main/kotlin/com/atomiccv/auth/application/port/TokenBlacklistPort.kt`:

```kotlin
package com.atomiccv.auth.application.port

import java.time.Duration

interface TokenBlacklistPort {
    fun add(token: String, ttl: Duration)
    fun isBlacklisted(token: String): Boolean
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew :module-auth:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/application/port/
git commit -m "feat(auth): Port 인터페이스 정의 (JwtPort, RefreshTokenPort, TokenBlacklistPort)"
```

---

## Task 3: OAuthLoginUseCase

**Files:**
- Create: `application/usecase/OAuthLoginUseCase.kt`
- Test: `test/.../application/OAuthLoginUseCaseTest.kt`

- [ ] **Step 1: 테스트 먼저 작성**

`module-auth/src/test/kotlin/com/atomiccv/auth/application/OAuthLoginUseCaseTest.kt`:

```kotlin
package com.atomiccv.auth.application

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.usecase.OAuthLoginCommand
import com.atomiccv.auth.application.usecase.OAuthLoginUseCase
import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import com.atomiccv.auth.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OAuthLoginUseCaseTest {

    private val userRepository: UserRepository = mockk()
    private val socialAccountRepository: SocialAccountRepository = mockk()
    private val jwtPort: JwtPort = mockk()
    private val refreshTokenPort: RefreshTokenPort = mockk()

    private val useCase = OAuthLoginUseCase(
        userRepository, socialAccountRepository, jwtPort, refreshTokenPort
    )

    private val command = OAuthLoginCommand(
        provider = SocialProvider.GOOGLE,
        providerUserId = "google-123",
        email = "test@example.com",
        name = "홍길동",
        profileImageUrl = "https://example.com/photo.jpg"
    )

    @Test
    fun `신규 사용자 소셜 로그인 시 User와 SocialAccount가 저장되고 토큰이 반환된다`() {
        val savedUser = User(id = 1L, email = command.email, name = command.name)
        val userSlot = slot<User>()
        val socialSlot = slot<SocialAccount>()

        every { socialAccountRepository.findByProviderAndProviderUserId(any(), any()) } returns null
        every { userRepository.findByEmail(command.email) } returns null
        every { userRepository.save(capture(userSlot)) } returns savedUser
        every { socialAccountRepository.save(capture(socialSlot)) } answers { firstArg() }
        every { jwtPort.generateAccessToken(1L) } returns "access-token"
        every { refreshTokenPort.save(1L, any(), any()) } returns Unit

        val result = useCase.login(command)

        assertEquals("access-token", result.accessToken)
        assertNotNull(result.refreshToken)
        assertEquals(command.email, userSlot.captured.email)
        assertEquals(SocialProvider.GOOGLE, socialSlot.captured.provider)
        assertEquals("google-123", socialSlot.captured.providerUserId)
    }

    @Test
    fun `재방문 사용자는 기존 User를 조회하고 새 토큰을 발급한다`() {
        val existingUser = User(id = 2L, email = command.email, name = command.name)
        val existingSocial = SocialAccount(
            id = 1L, userId = 2L,
            provider = SocialProvider.GOOGLE, providerUserId = "google-123"
        )

        every { socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-123") } returns existingSocial
        every { userRepository.findById(2L) } returns existingUser
        every { jwtPort.generateAccessToken(2L) } returns "access-token-2"
        every { refreshTokenPort.save(2L, any(), any()) } returns Unit

        val result = useCase.login(command)

        assertEquals("access-token-2", result.accessToken)
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { socialAccountRepository.save(any()) }
    }

    @Test
    fun `동일 이메일로 다른 제공자 로그인 시 기존 User에 SocialAccount가 추가된다`() {
        val existingUser = User(id = 3L, email = command.email, name = command.name)
        val kakaoCommand = command.copy(provider = SocialProvider.KAKAO, providerUserId = "kakao-456")
        val socialSlot = slot<SocialAccount>()

        every { socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-456") } returns null
        every { userRepository.findByEmail(command.email) } returns existingUser
        every { socialAccountRepository.save(capture(socialSlot)) } answers { firstArg() }
        every { jwtPort.generateAccessToken(3L) } returns "access-token-3"
        every { refreshTokenPort.save(3L, any(), any()) } returns Unit

        val result = useCase.login(kakaoCommand)

        assertEquals("access-token-3", result.accessToken)
        assertEquals(SocialProvider.KAKAO, socialSlot.captured.provider)
        assertEquals(3L, socialSlot.captured.userId)
        verify(exactly = 0) { userRepository.save(any()) }
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.application.OAuthLoginUseCaseTest"
```

Expected: FAIL — `OAuthLoginUseCase` 클래스 없음

- [ ] **Step 3: OAuthLoginUseCase 구현**

`module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/OAuthLoginUseCase.kt`:

```kotlin
package com.atomiccv.auth.application.usecase

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import com.atomiccv.auth.domain.repository.UserRepository
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

data class OAuthLoginCommand(
    val provider: SocialProvider,
    val providerUserId: String,
    val email: String,
    val name: String,
    val profileImageUrl: String?,
)

data class TokenResult(
    val accessToken: String,
    val refreshToken: String,
)

class OAuthLoginUseCase(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val jwtPort: JwtPort,
    private val refreshTokenPort: RefreshTokenPort,
) {
    @Transactional
    fun login(command: OAuthLoginCommand): TokenResult {
        val user = resolveUser(command)

        val accessToken = jwtPort.generateAccessToken(user.id)
        val refreshToken = UUID.randomUUID().toString()
        refreshTokenPort.save(user.id, refreshToken, Duration.ofDays(7))

        return TokenResult(accessToken, refreshToken)
    }

    private fun resolveUser(command: OAuthLoginCommand): User {
        // 1. 기존 소셜 계정으로 조회 (재방문)
        val existingSocial = socialAccountRepository.findByProviderAndProviderUserId(
            command.provider, command.providerUserId
        )
        if (existingSocial != null) {
            return userRepository.findById(existingSocial.userId)
                ?: error("SocialAccount가 참조하는 User가 없습니다: userId=${existingSocial.userId}")
        }

        // 2. 이메일로 기존 User 조회 (타 제공자 연동)
        val existingUser = userRepository.findByEmail(command.email)
        if (existingUser != null) {
            socialAccountRepository.save(
                SocialAccount(
                    userId = existingUser.id,
                    provider = command.provider,
                    providerUserId = command.providerUserId,
                )
            )
            return existingUser
        }

        // 3. 신규 가입
        val newUser = userRepository.save(
            User(
                email = command.email,
                name = command.name,
                profileImageUrl = command.profileImageUrl,
            )
        )
        socialAccountRepository.save(
            SocialAccount(
                userId = newUser.id,
                provider = command.provider,
                providerUserId = command.providerUserId,
            )
        )
        return newUser
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.application.OAuthLoginUseCaseTest"
```

Expected: 3 tests PASS

- [ ] **Step 5: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/OAuthLoginUseCase.kt \
        module-auth/src/test/kotlin/com/atomiccv/auth/application/OAuthLoginUseCaseTest.kt
git commit -m "feat(auth): OAuthLoginUseCase 구현 (신규 가입, 재방문, 계정 연동)"
```

---

## Task 4: TokenRefreshUseCase + LogoutUseCase

**Files:**
- Create: `application/usecase/TokenRefreshUseCase.kt`
- Create: `application/usecase/LogoutUseCase.kt`
- Test: `test/.../application/TokenRefreshUseCaseTest.kt`
- Test: `test/.../application/LogoutUseCaseTest.kt`

- [ ] **Step 1: TokenRefreshUseCase 테스트 작성**

`module-auth/src/test/kotlin/com/atomiccv/auth/application/TokenRefreshUseCaseTest.kt`:

```kotlin
package com.atomiccv.auth.application

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.test.assertEquals

class TokenRefreshUseCaseTest {

    private val refreshTokenPort: RefreshTokenPort = mockk()
    private val jwtPort: JwtPort = mockk()
    private val useCase = TokenRefreshUseCase(refreshTokenPort, jwtPort)

    @Test
    fun `유효한 Refresh Token으로 새 Access Token을 발급한다`() {
        every { refreshTokenPort.findUserIdByToken("valid-refresh") } returns 1L
        every { jwtPort.generateAccessToken(1L) } returns "new-access-token"

        val result = useCase.refresh("valid-refresh")

        assertEquals("new-access-token", result)
    }

    @Test
    fun `존재하지 않는 Refresh Token은 UNAUTHORIZED 예외를 발생시킨다`() {
        every { refreshTokenPort.findUserIdByToken("invalid") } returns null

        val ex = assertThrows<BusinessException> { useCase.refresh("invalid") }
        assertEquals(ErrorCode.UNAUTHORIZED, ex.errorCode)
    }
}
```

- [ ] **Step 2: LogoutUseCase 테스트 작성**

`module-auth/src/test/kotlin/com/atomiccv/auth/application/LogoutUseCaseTest.kt`:

```kotlin
package com.atomiccv.auth.application

import com.atomiccv.auth.application.port.JwtPort
import com.atomiccv.auth.application.port.RefreshTokenPort
import com.atomiccv.auth.application.port.TokenBlacklistPort
import com.atomiccv.auth.application.usecase.LogoutUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Date

class LogoutUseCaseTest {

    private val jwtPort: JwtPort = mockk()
    private val tokenBlacklistPort: TokenBlacklistPort = mockk()
    private val refreshTokenPort: RefreshTokenPort = mockk()
    private val useCase = LogoutUseCase(jwtPort, tokenBlacklistPort, refreshTokenPort)

    @Test
    fun `로그아웃 시 Access Token이 Blacklist에 등록되고 Refresh Token이 삭제된다`() {
        val expiration = Date(System.currentTimeMillis() + 3600_000)
        every { jwtPort.extractUserId("access-token") } returns 1L
        every { jwtPort.getRemainingTtl("access-token") } returns Duration.ofHours(1)
        every { tokenBlacklistPort.add("access-token", any()) } returns Unit
        every { refreshTokenPort.deleteByUserId(1L) } returns Unit

        useCase.logout("access-token")

        verify { tokenBlacklistPort.add("access-token", any()) }
        verify { refreshTokenPort.deleteByUserId(1L) }
    }
}
```

- [ ] **Step 3: 테스트 실행하여 실패 확인**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.application.TokenRefreshUseCaseTest" \
                             --tests "com.atomiccv.auth.application.LogoutUseCaseTest"
```

Expected: FAIL — 클래스 없음

- [ ] **Step 4: TokenRefreshUseCase 구현**

`module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/TokenRefreshUseCase.kt`:

```kotlin
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
        val userId = refreshTokenPort.findUserIdByToken(refreshToken)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        return jwtPort.generateAccessToken(userId)
    }
}
```

- [ ] **Step 5: LogoutUseCase 구현**

`module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/LogoutUseCase.kt`:

```kotlin
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
        val userId = jwtPort.extractUserId(accessToken)
        val remainingTtl = jwtPort.getRemainingTtl(accessToken)
        tokenBlacklistPort.add(accessToken, remainingTtl)
        refreshTokenPort.deleteByUserId(userId)
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.application.TokenRefreshUseCaseTest" \
                             --tests "com.atomiccv.auth.application.LogoutUseCaseTest"
```

Expected: 3 tests PASS

- [ ] **Step 7: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/application/usecase/ \
        module-auth/src/test/kotlin/com/atomiccv/auth/application/
git commit -m "feat(auth): TokenRefreshUseCase, LogoutUseCase 구현"
```

---

## Task 5: JwtProvider (JwtPort 구현체)

**Files:**
- Create: `infrastructure/client/JwtProvider.kt`
- Test: `test/.../infrastructure/JwtProviderTest.kt`

- [ ] **Step 1: JwtProvider 테스트 작성**

`module-auth/src/test/kotlin/com/atomiccv/auth/infrastructure/JwtProviderTest.kt`:

```kotlin
package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.JwtProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtProviderTest {

    // 테스트용 64자 이상 Base64 시크릿 (HS256 최소 256-bit)
    private val secret = "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHktbm90LWZvci1wcm9k"
    private val accessExpiryMs = 3_600_000L  // 1시간
    private val provider = JwtProvider(secret, accessExpiryMs)

    @Test
    fun `Access Token 발급 후 userId를 추출할 수 있다`() {
        val token = provider.generateAccessToken(42L)
        assertEquals(42L, provider.extractUserId(token))
    }

    @Test
    fun `유효한 토큰의 validateToken은 true를 반환한다`() {
        val token = provider.generateAccessToken(1L)
        assertTrue(provider.validateToken(token))
    }

    @Test
    fun `위변조된 토큰의 validateToken은 false를 반환한다`() {
        val token = provider.generateAccessToken(1L)
        val tampered = token.dropLast(5) + "XXXXX"
        assertFalse(provider.validateToken(tampered))
    }

    @Test
    fun `만료된 토큰의 validateToken은 false를 반환한다`() {
        val expiredProvider = JwtProvider(secret, -1000L)  // 이미 만료
        val token = expiredProvider.generateAccessToken(1L)
        assertFalse(provider.validateToken(token))
    }

    @Test
    fun `getRemainingTtl은 양수 Duration을 반환한다`() {
        val token = provider.generateAccessToken(1L)
        val ttl = provider.getRemainingTtl(token)
        assertTrue(ttl.toMillis() > 0)
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.infrastructure.JwtProviderTest"
```

Expected: FAIL — `JwtProvider` 클래스 없음

- [ ] **Step 3: JwtProvider 구현**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/JwtProvider.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

import com.atomiccv.auth.application.port.JwtPort
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-expiry-ms}") private val accessExpiryMs: Long,
) : JwtPort {

    private val key: SecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    override fun generateAccessToken(userId: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + accessExpiryMs))
            .signWith(key)
            .compact()
    }

    override fun validateToken(token: String): Boolean {
        return runCatching { parseClaims(token) }.isSuccess
    }

    override fun extractUserId(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    override fun getExpiration(token: String): Date {
        return parseClaims(token).expiration
    }

    override fun getRemainingTtl(token: String): Duration {
        val expiration = getExpiration(token)
        val remaining = expiration.time - System.currentTimeMillis()
        return if (remaining > 0) Duration.ofMillis(remaining) else Duration.ZERO
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.infrastructure.JwtProviderTest"
```

Expected: 5 tests PASS

- [ ] **Step 5: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/JwtProvider.kt \
        module-auth/src/test/kotlin/com/atomiccv/auth/infrastructure/JwtProviderTest.kt
git commit -m "feat(auth): JwtProvider 구현 (jjwt 0.12.6)"
```

---

## Task 6: Redis 어댑터

**Files:**
- Create: `infrastructure/client/RefreshTokenRedisAdapter.kt`
- Create: `infrastructure/client/TokenBlacklistRedisAdapter.kt`
- Test: `test/.../infrastructure/RefreshTokenRedisAdapterTest.kt`
- Test: `test/.../infrastructure/TokenBlacklistRedisAdapterTest.kt`

- [ ] **Step 1: RefreshTokenRedisAdapter 테스트 작성**

`module-auth/src/test/kotlin/com/atomiccv/auth/infrastructure/RefreshTokenRedisAdapterTest.kt`:

```kotlin
package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.RefreshTokenRedisAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RefreshTokenRedisAdapterTest {

    private val redisTemplate: StringRedisTemplate = mockk()
    private val valueOps: ValueOperations<String, String> = mockk()
    private val adapter = RefreshTokenRedisAdapter(redisTemplate)

    init {
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @Test
    fun `Refresh Token을 저장하면 'refresh:{token}' 키로 userId를 저장한다`() {
        every { valueOps.set("refresh:my-token", "10", Duration.ofDays(7)) } returns Unit

        adapter.save(10L, "my-token", Duration.ofDays(7))

        verify { valueOps.set("refresh:my-token", "10", Duration.ofDays(7)) }
    }

    @Test
    fun `저장된 Refresh Token으로 userId를 조회할 수 있다`() {
        every { valueOps.get("refresh:my-token") } returns "10"

        val userId = adapter.findUserIdByToken("my-token")

        assertEquals(10L, userId)
    }

    @Test
    fun `존재하지 않는 Refresh Token 조회 시 null을 반환한다`() {
        every { valueOps.get("refresh:missing") } returns null

        assertNull(adapter.findUserIdByToken("missing"))
    }

    @Test
    fun `deleteByUserId는 해당 userId 값을 가진 키를 삭제한다`() {
        // deleteByUserId는 userId 기반 prefix 키 삭제 방식 사용
        every { redisTemplate.delete("refresh-user:10") } returns true

        adapter.deleteByUserId(10L)

        verify { redisTemplate.delete("refresh-user:10") }
    }
}
```

- [ ] **Step 2: TokenBlacklistRedisAdapter 테스트 작성**

`module-auth/src/test/kotlin/com/atomiccv/auth/infrastructure/TokenBlacklistRedisAdapterTest.kt`:

```kotlin
package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.TokenBlacklistRedisAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenBlacklistRedisAdapterTest {

    private val redisTemplate: StringRedisTemplate = mockk()
    private val valueOps: ValueOperations<String, String> = mockk()
    private val adapter = TokenBlacklistRedisAdapter(redisTemplate)

    init {
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @Test
    fun `Blacklist에 토큰을 추가하면 'blacklist:{token}' 키로 저장된다`() {
        every { valueOps.set("blacklist:my-token", "1", Duration.ofHours(1)) } returns Unit

        adapter.add("my-token", Duration.ofHours(1))

        verify { valueOps.set("blacklist:my-token", "1", Duration.ofHours(1)) }
    }

    @Test
    fun `Blacklist에 있는 토큰은 isBlacklisted가 true를 반환한다`() {
        every { redisTemplate.hasKey("blacklist:my-token") } returns true

        assertTrue(adapter.isBlacklisted("my-token"))
    }

    @Test
    fun `Blacklist에 없는 토큰은 isBlacklisted가 false를 반환한다`() {
        every { redisTemplate.hasKey("blacklist:missing") } returns false

        assertFalse(adapter.isBlacklisted("missing"))
    }
}
```

- [ ] **Step 3: 테스트 실행하여 실패 확인**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.infrastructure.RefreshTokenRedisAdapterTest" \
                             --tests "com.atomiccv.auth.infrastructure.TokenBlacklistRedisAdapterTest"
```

Expected: FAIL — 클래스 없음

- [ ] **Step 4: RefreshTokenRedisAdapter 구현**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/RefreshTokenRedisAdapter.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

import com.atomiccv.auth.application.port.RefreshTokenPort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenRedisAdapter(
    private val redisTemplate: StringRedisTemplate,
) : RefreshTokenPort {

    override fun save(userId: Long, token: String, ttl: Duration) {
        redisTemplate.opsForValue().set(tokenKey(token), userId.toString(), ttl)
        // userId 기반 삭제를 위해 역인덱스도 저장
        redisTemplate.opsForValue().set(userKey(userId), token, ttl)
    }

    override fun findUserIdByToken(token: String): Long? {
        return redisTemplate.opsForValue().get(tokenKey(token))?.toLong()
    }

    override fun deleteByUserId(userId: Long) {
        // userId 기반 키 삭제 (token 키는 TTL로 자동 만료)
        redisTemplate.delete(userKey(userId))
    }

    private fun tokenKey(token: String) = "refresh:$token"
    private fun userKey(userId: Long) = "refresh-user:$userId"
}
```

- [ ] **Step 5: TokenBlacklistRedisAdapter 구현**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/TokenBlacklistRedisAdapter.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

import com.atomiccv.auth.application.port.TokenBlacklistPort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TokenBlacklistRedisAdapter(
    private val redisTemplate: StringRedisTemplate,
) : TokenBlacklistPort {

    override fun add(token: String, ttl: Duration) {
        redisTemplate.opsForValue().set(key(token), "1", ttl)
    }

    override fun isBlacklisted(token: String): Boolean {
        return redisTemplate.hasKey(key(token)) == true
    }

    private fun key(token: String) = "blacklist:$token"
}
```

- [ ] **Step 6: 테스트 통과 확인**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.infrastructure.RefreshTokenRedisAdapterTest" \
                             --tests "com.atomiccv.auth.infrastructure.TokenBlacklistRedisAdapterTest"
```

Expected: 6 tests PASS

- [ ] **Step 7: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/RefreshTokenRedisAdapter.kt \
        module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/TokenBlacklistRedisAdapter.kt \
        module-auth/src/test/kotlin/com/atomiccv/auth/infrastructure/
git commit -m "feat(auth): Redis 어댑터 구현 (RefreshToken, Blacklist)"
```

---

## Task 7: OAuth2UserInfo 계층 + Factory

**Files:**
- Create: `infrastructure/client/OAuth2UserInfo.kt`
- Create: `infrastructure/client/GoogleOAuth2UserInfo.kt`
- Create: `infrastructure/client/KakaoOAuth2UserInfo.kt`
- Create: `infrastructure/client/NaverOAuth2UserInfo.kt`
- Create: `infrastructure/client/OAuth2UserInfoFactory.kt`
- Test: `test/.../infrastructure/OAuth2UserInfoFactoryTest.kt`

- [ ] **Step 1: OAuth2UserInfo 인터페이스 + 구현체 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/OAuth2UserInfo.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

interface OAuth2UserInfo {
    fun getId(): String
    fun getEmail(): String
    fun getName(): String
    fun getProfileImageUrl(): String?
}
```

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/GoogleOAuth2UserInfo.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

class GoogleOAuth2UserInfo(private val attributes: Map<String, Any>) : OAuth2UserInfo {
    override fun getId(): String = attributes["sub"] as String
    override fun getEmail(): String = attributes["email"] as String
    override fun getName(): String = attributes["name"] as String
    override fun getProfileImageUrl(): String? = attributes["picture"] as? String
}
```

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/KakaoOAuth2UserInfo.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

class KakaoOAuth2UserInfo(private val attributes: Map<String, Any>) : OAuth2UserInfo {

    private val kakaoAccount: Map<*, *>
        get() = attributes["kakao_account"] as Map<*, *>

    private val profile: Map<*, *>
        get() = kakaoAccount["profile"] as Map<*, *>

    override fun getId(): String = attributes["id"].toString()
    override fun getEmail(): String = kakaoAccount["email"] as String
    override fun getName(): String = profile["nickname"] as String
    override fun getProfileImageUrl(): String? = profile["profile_image_url"] as? String
}
```

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/NaverOAuth2UserInfo.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

class NaverOAuth2UserInfo(private val attributes: Map<String, Any>) : OAuth2UserInfo {

    private val response: Map<*, *>
        get() = attributes["response"] as Map<*, *>

    override fun getId(): String = response["id"] as String
    override fun getEmail(): String = response["email"] as String
    override fun getName(): String = response["name"] as String
    override fun getProfileImageUrl(): String? = response["profile_image"] as? String
}
```

- [ ] **Step 2: OAuth2UserInfoFactory 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/OAuth2UserInfoFactory.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

object OAuth2UserInfoFactory {
    fun of(registrationId: String, attributes: Map<String, Any>): OAuth2UserInfo {
        return when (registrationId.lowercase()) {
            "google" -> GoogleOAuth2UserInfo(attributes)
            "kakao" -> KakaoOAuth2UserInfo(attributes)
            "naver" -> NaverOAuth2UserInfo(attributes)
            else -> throw IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다: $registrationId")
        }
    }
}
```

- [ ] **Step 3: Factory 테스트 작성**

`module-auth/src/test/kotlin/com/atomiccv/auth/infrastructure/OAuth2UserInfoFactoryTest.kt`:

```kotlin
package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.OAuth2UserInfoFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class OAuth2UserInfoFactoryTest {

    @Test
    fun `google registrationId로 GoogleOAuth2UserInfo를 생성한다`() {
        val attrs = mapOf("sub" to "g-123", "email" to "a@gmail.com", "name" to "홍길동", "picture" to "https://photo")
        val info = OAuth2UserInfoFactory.of("google", attrs)
        assertEquals("g-123", info.getId())
        assertEquals("a@gmail.com", info.getEmail())
        assertEquals("홍길동", info.getName())
        assertEquals("https://photo", info.getProfileImageUrl())
    }

    @Test
    fun `kakao registrationId로 KakaoOAuth2UserInfo를 생성한다`() {
        val attrs = mapOf(
            "id" to 99999L,
            "kakao_account" to mapOf(
                "email" to "b@kakao.com",
                "profile" to mapOf("nickname" to "카카오유저", "profile_image_url" to "https://kakao-photo")
            )
        )
        val info = OAuth2UserInfoFactory.of("kakao", attrs)
        assertEquals("99999", info.getId())
        assertEquals("b@kakao.com", info.getEmail())
        assertEquals("카카오유저", info.getName())
    }

    @Test
    fun `naver registrationId로 NaverOAuth2UserInfo를 생성한다`() {
        val attrs = mapOf(
            "response" to mapOf(
                "id" to "n-456",
                "email" to "c@naver.com",
                "name" to "네이버유저",
                "profile_image" to "https://naver-photo"
            )
        )
        val info = OAuth2UserInfoFactory.of("naver", attrs)
        assertEquals("n-456", info.getId())
        assertEquals("c@naver.com", info.getEmail())
    }

    @Test
    fun `지원하지 않는 provider는 예외를 발생시킨다`() {
        assertThrows<IllegalArgumentException> {
            OAuth2UserInfoFactory.of("github", emptyMap())
        }
    }
}
```

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.infrastructure.OAuth2UserInfoFactoryTest"
```

Expected: 4 tests PASS

- [ ] **Step 5: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/OAuth2UserInfo.kt \
        module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/GoogleOAuth2UserInfo.kt \
        module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/KakaoOAuth2UserInfo.kt \
        module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/NaverOAuth2UserInfo.kt \
        module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/OAuth2UserInfoFactory.kt \
        module-auth/src/test/kotlin/com/atomiccv/auth/infrastructure/OAuth2UserInfoFactoryTest.kt
git commit -m "feat(auth): OAuth2UserInfo 계층 구현 (Google, Kakao, Naver, Factory)"
```

---

## Task 8: JPA 엔티티 + Repository 구현체

**Files:**
- Create: `infrastructure/persistence/UserJpaEntity.kt`
- Create: `infrastructure/persistence/SocialAccountJpaEntity.kt`
- Create: `infrastructure/persistence/UserJpaRepository.kt`
- Create: `infrastructure/persistence/SocialAccountJpaRepository.kt`
- Create: `infrastructure/persistence/UserRepositoryImpl.kt`
- Create: `infrastructure/persistence/SocialAccountRepositoryImpl.kt`

- [ ] **Step 1: UserJpaEntity 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/persistence/UserJpaEntity.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.model.UserRole
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val name: String,

    @Column(name = "profile_image_url")
    val profileImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = User(
        id = id,
        email = email,
        name = name,
        profileImageUrl = profileImageUrl,
        role = role,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(user: User) = UserJpaEntity(
            id = user.id,
            email = user.email,
            name = user.name,
            profileImageUrl = user.profileImageUrl,
            role = user.role,
            isActive = user.isActive,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }
}
```

- [ ] **Step 2: SocialAccountJpaEntity 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/persistence/SocialAccountJpaEntity.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_user_id"])]
)
class SocialAccountJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: SocialProvider,

    @Column(name = "provider_user_id", nullable = false)
    val providerUserId: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = SocialAccount(
        id = id,
        userId = userId,
        provider = provider,
        providerUserId = providerUserId,
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(account: SocialAccount) = SocialAccountJpaEntity(
            id = account.id,
            userId = account.userId,
            provider = account.provider,
            providerUserId = account.providerUserId,
            createdAt = account.createdAt,
        )
    }
}
```

- [ ] **Step 3: Spring Data JPA 인터페이스 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/persistence/UserJpaRepository.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByEmail(email: String): UserJpaEntity?
    fun existsByEmail(email: String): Boolean
}
```

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/persistence/SocialAccountJpaRepository.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.SocialProvider
import org.springframework.data.jpa.repository.JpaRepository

interface SocialAccountJpaRepository : JpaRepository<SocialAccountJpaEntity, Long> {
    fun findByProviderAndProviderUserId(provider: SocialProvider, providerUserId: String): SocialAccountJpaEntity?
    fun findAllByUserId(userId: Long): List<SocialAccountJpaEntity>
}
```

- [ ] **Step 4: Repository 구현체 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/persistence/UserRepositoryImpl.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): User =
        jpaRepository.save(UserJpaEntity.fromDomain(user)).toDomain()

    override fun findById(id: Long): User? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? =
        jpaRepository.findByEmail(email)?.toDomain()

    override fun existsByEmail(email: String): Boolean =
        jpaRepository.existsByEmail(email)
}
```

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/persistence/SocialAccountRepositoryImpl.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.persistence

import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import com.atomiccv.auth.domain.repository.SocialAccountRepository
import org.springframework.stereotype.Repository

@Repository
class SocialAccountRepositoryImpl(
    private val jpaRepository: SocialAccountJpaRepository,
) : SocialAccountRepository {
    override fun save(socialAccount: SocialAccount): SocialAccount =
        jpaRepository.save(SocialAccountJpaEntity.fromDomain(socialAccount)).toDomain()

    override fun findByProviderAndProviderUserId(provider: SocialProvider, providerUserId: String): SocialAccount? =
        jpaRepository.findByProviderAndProviderUserId(provider, providerUserId)?.toDomain()

    override fun findAllByUserId(userId: Long): List<SocialAccount> =
        jpaRepository.findAllByUserId(userId).map { it.toDomain() }
}
```

- [ ] **Step 5: 빌드 확인**

```bash
./gradlew :module-auth:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/persistence/
git commit -m "feat(auth): JPA 엔티티 및 Repository 구현체 작성"
```

---

## Task 9: Spring Security 설정

**Files:**
- Create: `infrastructure/client/CustomOAuth2UserService.kt`
- Create: `infrastructure/client/OAuth2AuthenticationSuccessHandler.kt`
- Create: `infrastructure/SecurityConfig.kt`

- [ ] **Step 1: CustomOAuth2UserService 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/CustomOAuth2UserService.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

import com.atomiccv.auth.application.usecase.OAuthLoginCommand
import com.atomiccv.auth.application.usecase.OAuthLoginUseCase
import com.atomiccv.auth.domain.model.SocialProvider
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val oAuthLoginUseCase: OAuthLoginUseCase,
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId
        val userInfo = OAuth2UserInfoFactory.of(registrationId, oAuth2User.attributes)

        val provider = SocialProvider.valueOf(registrationId.uppercase())
        val command = OAuthLoginCommand(
            provider = provider,
            providerUserId = userInfo.getId(),
            email = userInfo.getEmail(),
            name = userInfo.getName(),
            profileImageUrl = userInfo.getProfileImageUrl(),
        )

        // 로그인/회원가입 처리 — TokenResult는 SuccessHandler에서 사용하기 위해 attribute에 보관
        val tokenResult = oAuthLoginUseCase.login(command)
        return OAuth2UserWithToken(oAuth2User, tokenResult.accessToken, tokenResult.refreshToken)
    }
}
```

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/OAuth2UserWithToken.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class OAuth2UserWithToken(
    private val delegate: OAuth2User,
    val accessToken: String,
    val refreshToken: String,
) : OAuth2User by delegate
```

- [ ] **Step 2: OAuth2AuthenticationSuccessHandler 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/client/OAuth2AuthenticationSuccessHandler.kt`:

```kotlin
package com.atomiccv.auth.infrastructure.client

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2AuthenticationSuccessHandler(
    @Value("\${app.frontend-url}") private val frontendUrl: String,
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val user = authentication.principal as OAuth2UserWithToken
        addCookie(response, "access_token", user.accessToken, 3600)
        addCookie(response, "refresh_token", user.refreshToken, 7 * 24 * 3600, "/api/auth/refresh")
        redirectStrategy.sendRedirect(request, response, frontendUrl)
    }

    private fun addCookie(
        response: HttpServletResponse,
        name: String,
        value: String,
        maxAgeSeconds: Int,
        path: String = "/",
    ) {
        val cookie = Cookie(name, value).apply {
            isHttpOnly = true
            secure = true
            this.path = path
            maxAge = maxAgeSeconds
        }
        response.addCookie(cookie)
    }
}
```

- [ ] **Step 3: SecurityConfig 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/SecurityConfig.kt`:

```kotlin
package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.CustomOAuth2UserService
import com.atomiccv.auth.infrastructure.client.OAuth2AuthenticationSuccessHandler
import com.atomiccv.auth.interfaces.rest.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/oauth2/**",
                    "/login/**",
                    "/actuator/health",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2Login {
                it.userInfoEndpoint { endpoint -> endpoint.userService(customOAuth2UserService) }
                it.successHandler(oAuth2AuthenticationSuccessHandler)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew :module-auth:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/
git commit -m "feat(auth): Spring Security 설정 (OAuth2, JWT Filter, SecurityConfig)"
```

---

## Task 10: AuthController + JwtAuthenticationFilter + GlobalExceptionHandler

**Files:**
- Create: `interfaces/rest/JwtAuthenticationFilter.kt`
- Create: `interfaces/rest/AuthController.kt`
- Create: `interfaces/rest/GlobalExceptionHandler.kt`
- Test: `test/.../interfaces/AuthControllerTest.kt`

- [ ] **Step 1: JwtAuthenticationFilter 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/JwtAuthenticationFilter.kt`:

```kotlin
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
            val auth = UsernamePasswordAuthenticationToken(
                userId, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
            )
            SecurityContextHolder.getContext().authentication = auth
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        return request.cookies?.firstOrNull { it.name == "access_token" }?.value
    }
}
```

- [ ] **Step 2: AuthController 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/AuthController.kt`:

```kotlin
package com.atomiccv.auth.interfaces.rest

import com.atomiccv.auth.application.usecase.LogoutUseCase
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val tokenRefreshUseCase: TokenRefreshUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userRepository: UserRepository,
) {
    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<ApiResponse<Nothing>> {
        val refreshToken = request.cookies?.firstOrNull { it.name == "refresh_token" }?.value
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        val newAccessToken = tokenRefreshUseCase.refresh(refreshToken)
        response.addCookie(Cookie("access_token", newAccessToken).apply {
            isHttpOnly = true
            secure = true
            path = "/"
            maxAge = 3600
        })
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<ApiResponse<Nothing>> {
        val accessToken = request.cookies?.firstOrNull { it.name == "access_token" }?.value
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        logoutUseCase.logout(accessToken)

        listOf("access_token", "refresh_token").forEach { cookieName ->
            response.addCookie(Cookie(cookieName, "").apply {
                isHttpOnly = true
                secure = true
                path = "/"
                maxAge = 0
            })
        }
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userRepository.findById(userId)
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다.")
        return ResponseEntity.ok(ApiResponse.ok(UserResponse(user.id, user.email, user.name, user.profileImageUrl)))
    }
}

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val profileImageUrl: String?,
)
```

- [ ] **Step 3: GlobalExceptionHandler 작성**

`module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/rest/GlobalExceptionHandler.kt`:

```kotlin
package com.atomiccv.auth.interfaces.rest

import com.atomiccv.shared.common.exception.BusinessException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(e.httpStatus).body(
            ErrorResponse(
                code = e.code,
                message = e.message,
                timestamp = LocalDateTime.now(),
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(500).body(
            ErrorResponse(
                code = "INTERNAL_SERVER_ERROR",
                message = "서버 내부 오류가 발생했습니다.",
                timestamp = LocalDateTime.now(),
            )
        )
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime,
)
```

- [ ] **Step 4: AuthController @WebMvcTest 작성**

`module-auth/src/test/kotlin/com/atomiccv/auth/interfaces/AuthControllerTest.kt`:

```kotlin
package com.atomiccv.auth.interfaces

import com.atomiccv.auth.application.usecase.LogoutUseCase
import com.atomiccv.auth.application.usecase.TokenRefreshUseCase
import com.atomiccv.auth.domain.model.User
import com.atomiccv.auth.domain.repository.UserRepository
import com.atomiccv.auth.interfaces.rest.AuthController
import com.atomiccv.auth.interfaces.rest.GlobalExceptionHandler
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get

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
        @Bean fun tokenRefreshUseCase(): TokenRefreshUseCase = mockk()
        @Bean fun logoutUseCase(): LogoutUseCase = mockk()
        @Bean fun userRepository(): UserRepository = mockk()
    }

    @Test
    @WithMockUser
    fun `POST refresh — 유효한 Refresh Token Cookie로 새 Access Token Cookie를 발급한다`() {
        every { tokenRefreshUseCase.refresh("valid-refresh") } returns "new-access"

        mockMvc.post("/api/auth/refresh") {
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
        mockMvc.post("/api/auth/refresh") {
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @WithMockUser
    fun `POST logout — Access Token Cookie가 있으면 로그아웃하고 Cookie를 삭제한다`() {
        every { logoutUseCase.logout("my-token") } returns Unit

        mockMvc.post("/api/auth/logout") {
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
```

- [ ] **Step 5: 테스트 실행**

```bash
./gradlew :module-auth:test --tests "com.atomiccv.auth.interfaces.AuthControllerTest"
```

Expected: 4 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add module-auth/src/main/kotlin/com/atomiccv/auth/interfaces/ \
        module-auth/src/test/kotlin/com/atomiccv/auth/interfaces/
git commit -m "feat(auth): AuthController, JwtAuthenticationFilter, GlobalExceptionHandler 구현"
```

---

## Task 11: 전체 테스트 + application.yml 설정

**Files:**
- Create: `module-auth/src/test/resources/application-test.yml`
- Modify: `module-auth/src/main/resources/application.yml` (환경변수 플레이스홀더 확인)

- [ ] **Step 1: 테스트용 application-test.yml 작성**

`module-auth/src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  data:
    redis:
      host: localhost
      port: 6379
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test-google-id
            client-secret: test-google-secret
            scope: email,profile
          kakao:
            client-id: test-kakao-id
            client-secret: test-kakao-secret
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            scope: account_email,profile_nickname,profile_image
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          naver:
            client-id: test-naver-id
            client-secret: test-naver-secret
            authorization-grant-type: authorization_code
            scope: email,name,profile_image
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response

jwt:
  secret: dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHktbm90LWZvci1wcm9k
  access-expiry-ms: 3600000

app:
  frontend-url: http://localhost:3000
```

- [ ] **Step 2: 전체 테스트 실행**

```bash
./gradlew :module-auth:test
```

Expected: 전체 PASS, BUILD SUCCESSFUL

- [ ] **Step 3: TASKS.md Phase 1-3 상태 업데이트**

`doc/TASKS.md`의 Phase 1-3 항목 상태를 🟢로 변경.

- [ ] **Step 4: 최종 커밋**

```bash
git add module-auth/src/test/resources/application-test.yml \
        doc/TASKS.md
git commit -m "feat(auth): module-auth 구현 완료 — 전체 테스트 통과"
```

---

## 자가 검토

- **스펙 커버리지:** Google/Kakao/Naver 3종 OAuth2, HttpOnly Cookie, 동일 이메일 자동 연동, JWT+Blacklist, /refresh, /logout, /me 전부 포함 ✓
- **Port 인터페이스 일관성:** `JwtPort.getRemainingTtl()` — Task 2에서 정의, Task 4(LogoutUseCase)와 Task 5(JwtProvider)에서 동일 시그니처 사용 ✓
- **타입 일관성:** `OAuthLoginCommand`, `TokenResult` — Task 3에서 정의, Task 9(CustomOAuth2UserService)에서 동일하게 사용 ✓
- **Placeholder 없음:** 모든 단계에 실제 코드 포함 ✓
