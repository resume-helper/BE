# Auth 모듈 설계 스펙 — Phase 1-3

> 작성일: 2026-05-02  
> 상태: 확정  
> 담당 모듈: `:module-auth`

---

## 1. 개요

Atomic CV의 인증은 **소셜 로그인 전용**으로 구현한다.  
일반 로그인(이메일 + 비밀번호)과 이메일 인증은 이번 Phase에서 제공하지 않는다.

| 항목 | 결정 |
|------|------|
| 로그인 방식 | OAuth2 소셜 로그인 (Google / Kakao / Naver) |
| 일반 로그인 | 미사용 |
| 이메일 인증 | 미사용 |
| 토큰 전달 방식 | HttpOnly Cookie (XSS 방어) |
| 동일 이메일 계정 처리 | 자동 연동 (같은 이메일이면 하나의 User로 통합) |
| 토큰 저장소 | Redis (Refresh Token 저장, Blacklist 관리) |
| 아키텍처 | Hexagonal Architecture (DDD) |

---

## 2. 도메인 모델

```
User
├── id: Long (PK)
├── email: String (UNIQUE)
├── name: String
├── profileImageUrl: String?
├── role: UserRole (USER / ADMIN)
├── isActive: Boolean (DEFAULT TRUE)
├── createdAt: LocalDateTime
└── updatedAt: LocalDateTime

SocialAccount
├── id: Long (PK)
├── userId: Long (FK → User)
├── provider: SocialProvider (GOOGLE / KAKAO / NAVER)
├── providerUserId: String
└── createdAt: LocalDateTime
(UNIQUE: provider + providerUserId)
```

**계정 연동 규칙**
- 소셜 로그인 시 이메일로 기존 User 조회
- 존재하면 → 해당 User에 SocialAccount 추가 (기존 계정 연동)
- 없으면 → User + SocialAccount 신규 생성

---

## 3. 컴포넌트 구조

Hexagonal Architecture를 따르며, Application 레이어가 Infrastructure에 의존하지 않도록 Port Interface를 둔다.

### 3-1. Domain

```
domain/model/
  User.kt                    # 유저 엔티티 (순수 Kotlin, 외부 라이브러리 금지)
  SocialAccount.kt           # 소셜 계정 연결 엔티티
  SocialProvider.kt          # enum: GOOGLE, KAKAO, NAVER
  UserRole.kt                # enum: USER, ADMIN

domain/repository/
  UserRepository.kt          # 인터페이스 (findByEmail, save 등)
  SocialAccountRepository.kt # 인터페이스 (findByProviderAndProviderUserId 등)
```

### 3-2. Application

```
application/usecase/
  OAuthLoginUseCase.kt       # 소셜 로그인: 신규 가입 or 기존 연동 처리 → 토큰 발급
  TokenRefreshUseCase.kt     # Refresh Token 검증 → Access Token 재발급
  LogoutUseCase.kt           # Access Token Blacklist 등록 + Refresh Token 삭제

application/port/
  JwtPort.kt                 # generateAccessToken, validateToken, extractUserId
  RefreshTokenPort.kt        # save, find, delete
  TokenBlacklistPort.kt      # add, isBlacklisted
```

### 3-3. Infrastructure

```
infrastructure/persistence/
  UserRepositoryImpl.kt
  SocialAccountRepositoryImpl.kt

infrastructure/client/
  JwtProvider.kt             # JwtPort 구현 (jjwt)
  RefreshTokenRedisAdapter.kt  # RefreshTokenPort 구현 (Redis)
  TokenBlacklistRedisAdapter.kt # TokenBlacklistPort 구현 (Redis)

  OAuth2UserInfo.kt          # interface: getId, getEmail, getName, getProfileImage
  GoogleOAuth2UserInfo.kt
  KakaoOAuth2UserInfo.kt
  NaverOAuth2UserInfo.kt
  OAuth2UserInfoFactory.kt   # provider 문자열 → OAuth2UserInfo 인스턴스 반환

  CustomOAuth2UserService.kt          # OAuth2UserDetailsService 구현
  OAuth2AuthenticationSuccessHandler.kt # 로그인 성공 시 토큰 발급 + Cookie 설정
  SecurityConfig.kt
```

### 3-4. Interfaces

```
interfaces/rest/
  AuthController.kt          # /api/auth/refresh, /api/auth/logout, /api/auth/me
  JwtAuthenticationFilter.kt # 요청마다 Access Token 검증
  GlobalExceptionHandler.kt  # @RestControllerAdvice
```

---

## 4. API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/oauth2/authorization/{provider}` | 소셜 로그인 시작 (Spring Security 자동 처리) | 없음 |
| `GET` | `/login/oauth2/code/{provider}` | OAuth2 콜백 (Spring Security 자동 처리) | 없음 |
| `POST` | `/api/auth/refresh` | Access Token 재발급 | Refresh Cookie |
| `POST` | `/api/auth/logout` | 로그아웃 (Blacklist 등록 + Cookie 삭제) | Access Token |
| `GET` | `/api/auth/me` | 현재 로그인 유저 정보 조회 | Access Token |

---

## 5. 토큰 흐름

```
[소셜 로그인 성공]
OAuth2AuthenticationSuccessHandler
  → OAuthLoginUseCase.login(provider, OAuth2UserInfo)
      → UserRepository.findByEmail()
      → 신규: User 생성 + SocialAccount 생성
      → 재방문: SocialAccount 연동 확인 (없으면 추가)
      → JwtPort.generateAccessToken(userId)   → Set-Cookie: access_token; HttpOnly; Secure
      → RefreshTokenPort.save(userId, token)  → Set-Cookie: refresh_token; HttpOnly; Secure; Path=/api/auth/refresh

[토큰 갱신]
POST /api/auth/refresh
  → TokenRefreshUseCase
      → RefreshTokenPort.find(token)
      → JwtPort.generateAccessToken(userId)
      → 새 Access Token Cookie 설정

[로그아웃]
POST /api/auth/logout
  → LogoutUseCase
      → TokenBlacklistPort.add(accessToken, remainingTtl)
      → RefreshTokenPort.delete(userId)
      → Cookie 삭제 (Max-Age=0)
```

---

## 6. 에러 처리

| 상황 | 에러코드 | HTTP |
|------|----------|------|
| OAuth2 제공자 응답 오류 | `OAUTH2_PROVIDER_ERROR` | 502 |
| Access Token 만료 | `TOKEN_EXPIRED` | 401 |
| 토큰 위변조 / 형식 오류 | `INVALID_TOKEN` | 401 |
| Blacklist 토큰 재사용 | `UNAUTHORIZED` | 401 |
| 비활성 유저 접근 | `FORBIDDEN` | 403 |

모든 예외는 `BusinessException`을 상속하며 `GlobalExceptionHandler`에서 일괄 처리한다.

---

## 7. 테스트 전략

| 레이어 | 테스트 대상 | 방식 |
|--------|------------|------|
| Domain | User / SocialAccount 생성 및 상태 검증 | 단위 테스트 (순수 Kotlin) |
| Application | `OAuthLoginUseCase` — 신규 가입, 재방문, 계정 연동 3 시나리오 | MockK (Port mock) |
| Application | `TokenRefreshUseCase`, `LogoutUseCase` | MockK |
| Infrastructure | `JwtProvider` 발급 / 검증 / 만료 처리 | 단위 테스트 |
| Interfaces | `AuthController` `/refresh`, `/logout`, `/me` | `@WebMvcTest` + MockK |

---

## 8. 비회원 체험 기능 (향후 고려)

이번 Phase에서는 구현하지 않는다.  
추후 추가 시 현재 auth 구조 변경 없이 다음 중 택일한다:

- **A안 (Guest JWT):** `UserRole.GUEST` 추가 + `GuestTokenUseCase` 신규 작성
- **B안 (Redis 핑거프린트):** auth 모듈 무관, `module-resume`에서 독립 처리

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-05-02 | 최초 작성 |
