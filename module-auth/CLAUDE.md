# CLAUDE.md — module-auth

> 이 파일은 `:module-auth` 작업 시에만 적용되는 설계 명세 및 규칙이다.
> 프로젝트 공통 규칙: 루트 `CLAUDE.md` 참조

---

## 모듈 역할

소셜 로그인(OAuth2) 기반 인증, JWT 토큰 발급/검증, Redis Blacklist 관리를 담당한다.

---

## 설계 결정 사항

| 항목 | 결정 |
|------|------|
| 로그인 방식 | OAuth2 소셜 로그인 (Google / Kakao / Naver) |
| 일반 로그인 | 미사용 |
| 이메일 인증 | 미사용 |
| 토큰 전달 방식 | HttpOnly Cookie (XSS 방어) |
| 동일 이메일 계정 처리 | 자동 연동 (같은 이메일이면 하나의 User로 통합) |
| 토큰 저장소 | Redis (Refresh Token 저장, Blacklist 관리) |

---

## 도메인 모델

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

## 컴포넌트 구조

### Domain
```
domain/model/
  User.kt
  SocialAccount.kt
  SocialProvider.kt          # enum: GOOGLE, KAKAO, NAVER
  UserRole.kt                # enum: USER, ADMIN

domain/repository/
  UserRepository.kt
  SocialAccountRepository.kt
```

### Application
```
application/usecase/
  OAuthLoginUseCase.kt       # 신규 가입 or 기존 연동 처리 → 토큰 발급
  TokenRefreshUseCase.kt     # Refresh Token 검증 → Access Token 재발급
  LogoutUseCase.kt           # Access Token Blacklist 등록 + Refresh Token 삭제

application/port/
  JwtPort.kt                 # generateAccessToken, validateToken, extractUserId
  RefreshTokenPort.kt        # save, find, delete
  TokenBlacklistPort.kt      # add, isBlacklisted
```

### Infrastructure
```
infrastructure/persistence/
  UserRepositoryImpl.kt
  SocialAccountRepositoryImpl.kt

# 공통 베이스 (module-shared)
shared/infrastructure/persistence/
  BaseJpaEntity.kt    # @MappedSuperclass — createdAt + updatedAt JPA Auditing 자동 관리
                      # UserJpaEntity 등 createdAt/updatedAt이 모두 필요한 엔티티가 상속
                      # createdAt만 필요한 엔티티(SocialAccountJpaEntity)는 @CreatedDate 직접 선언

infrastructure/client/
  JwtProvider.kt                        # JwtPort 구현
  RefreshTokenRedisAdapter.kt           # RefreshTokenPort 구현
  TokenBlacklistRedisAdapter.kt         # TokenBlacklistPort 구현

  OAuth2UserInfo.kt                     # interface
  GoogleOAuth2UserInfo.kt
  KakaoOAuth2UserInfo.kt
  NaverOAuth2UserInfo.kt
  OAuth2UserInfoFactory.kt              # provider → OAuth2UserInfo 반환

  CustomOAuth2UserService.kt
  OAuth2AuthenticationSuccessHandler.kt
  SecurityConfig.kt
```

### Interfaces
```
interfaces/rest/
  AuthController.kt          # /api/auth/refresh, /api/auth/logout, /api/auth/me
  JwtAuthenticationFilter.kt
  GlobalExceptionHandler.kt
```

---

## API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/oauth2/authorization/{provider}` | 소셜 로그인 시작 | 없음 |
| `GET` | `/login/oauth2/code/{provider}` | OAuth2 콜백 | 없음 |
| `POST` | `/api/auth/refresh` | Access Token 재발급 | Refresh Cookie |
| `POST` | `/api/auth/logout` | 로그아웃 | Access Token |
| `GET` | `/api/auth/me` | 현재 유저 정보 | Access Token |

---

## 토큰 흐름

```
[로그인]
OAuth2AuthenticationSuccessHandler
  → OAuthLoginUseCase.login()
      → 신규: User + SocialAccount 생성
      → 재방문: SocialAccount 연동 확인 (없으면 추가)
      → Set-Cookie: access_token (1h); HttpOnly; Secure
      → Set-Cookie: refresh_token (7d); HttpOnly; Secure; Path=/api/auth/refresh

[토큰 갱신]
TokenRefreshUseCase → 새 Access Token Cookie 설정

[로그아웃]
LogoutUseCase
  → TokenBlacklistPort.add(accessToken)
  → RefreshTokenPort.delete(userId)
  → Cookie Max-Age=0
```

---

## 에러코드

| 상황 | 에러코드 | HTTP |
|------|----------|------|
| OAuth2 제공자 응답 오류 | `OAUTH2_PROVIDER_ERROR` | 502 |
| Access Token 만료 | `TOKEN_EXPIRED` | 401 |
| 토큰 위변조 / 형식 오류 | `INVALID_TOKEN` | 401 |
| Blacklist 토큰 재사용 | `UNAUTHORIZED` | 401 |
| 비활성 유저 접근 | `FORBIDDEN` | 403 |

---

## 테스트 전략

| 레이어 | 대상 | 방식 |
|--------|------|------|
| Domain | User / SocialAccount 생성 및 상태 검증 | 단위 테스트 |
| Application | OAuthLoginUseCase (신규/재방문/연동) | MockK (Port mock) |
| Application | TokenRefreshUseCase, LogoutUseCase | MockK |
| Infrastructure | JwtProvider 발급/검증/만료 | 단위 테스트 |
| Interfaces | AuthController 3개 엔드포인트 | `@WebMvcTest` + MockK |

---

## 향후 고려: 비회원 체험 기능

이번 Phase 미구현. 추후 아래 중 택일:
- **A안:** `UserRole.GUEST` 추가 + `GuestTokenUseCase` 신규 작성
- **B안:** auth 모듈 무관, `module-resume`에서 Redis 핑거프린트로 독립 처리
