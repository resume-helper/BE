# CLAUDE.md — module-auth

> 이 파일은 `:module-auth` 작업 시에만 적용되는 설계 명세 및 규칙이다.
> 프로젝트 공통 규칙: 루트 `CLAUDE.md` 참조

---

## 모듈 역할

NextAuth.js 기반 소셜 로그인의 백엔드 토큰 발급, JWT 검증, Redis Blacklist 관리, 회원 탈퇴를 담당한다.

---

## 설계 결정 사항

| 항목 | 결정 |
|------|------|
| 로그인 방식 | NextAuth.js (FE) → `POST /api/auth/social-login` (BE) |
| OAuth2 서버 흐름 | 미사용 (Spring Security OAuth2 Client 제거) |
| 일반 로그인 | 미사용 |
| 이메일 인증 | 미사용 |
| 토큰 전달 방식 | HttpOnly Cookie + SameSite=Lax (XSS + CSRF 방어) |
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
- social-login 요청 시 이메일로 기존 User 조회
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
  SocialLoginUseCase.kt      # social-login Command → OAuthLoginCommand 변환 → 위임
  OAuthLoginUseCase.kt       # 신규 가입/계정 연동/탈퇴 유예 복구 → 토큰 발급 (비즈니스 코어)
  TokenRefreshUseCase.kt     # Refresh Token 검증 → Access Token 재발급
  LogoutUseCase.kt           # Access Token Blacklist 등록 + Refresh Token 삭제
  WithdrawUseCase.kt         # 계정 비활성화 + 30일 후 영구 삭제 (유예기간 내 재로그인 시 복구)

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

infrastructure/client/
  JwtProvider.kt                        # JwtPort 구현
  RefreshTokenRedisAdapter.kt           # RefreshTokenPort 구현
  TokenBlacklistRedisAdapter.kt         # TokenBlacklistPort 구현

infrastructure/
  SecurityConfig.kt                     # JwtAuthenticationFilter + permitAll 매처
```

### Interfaces
```
interfaces/rest/
  AuthController.kt          # /api/auth/{social-login,refresh,logout,withdraw,me}
  JwtAuthenticationFilter.kt
  GlobalExceptionHandler.kt
```

---

## API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/api/auth/social-login` | NextAuth 전달 정보로 토큰 발급 | 없음 |
| `POST` | `/api/auth/refresh` | Access Token 재발급 | Refresh Cookie |
| `POST` | `/api/auth/logout` | 로그아웃 | Access Token |
| `DELETE` | `/api/auth/withdraw` | 회원 탈퇴 (30일 유예) | Access Token |
| `GET` | `/api/auth/me` | 현재 유저 정보 | Access Token |

---

## 토큰 흐름

```
[로그인]
프론트엔드 NextAuth.js
  → Google/Kakao/Naver OAuth 처리 (FE)
  → POST /api/auth/social-login { provider, providerUserId, email, name, profileImageUrl }
SocialLoginUseCase
  → 신규: User + SocialAccount 생성
  → 재방문: SocialAccount 연동 확인 (없으면 추가)
  → Set-Cookie: access_token (1h); HttpOnly; Secure; SameSite=Lax; Path=/
  → Set-Cookie: refresh_token (7d); HttpOnly; Secure; SameSite=Lax; Path=/api/auth/refresh

[토큰 갱신]
TokenRefreshUseCase → 새 Access Token Cookie 설정 (SameSite=Lax)

[로그아웃]
LogoutUseCase
  → validateToken() 실패 시 조용히 성공 (만료·위변조 토큰도 idempotent 처리)
  → TokenBlacklistPort.add(accessToken)
  → RefreshTokenPort.delete(userId)
  → Cookie Max-Age=0; SameSite=Lax
```

---

## 에러코드

| 상황 | 에러코드 | HTTP |
|------|----------|------|
| 지원하지 않는 provider / 입력값 누락 | `VALIDATION_FAILED` | 400 |
| Access Token 만료 | `TOKEN_EXPIRED` | 401 |
| 토큰 위변조 / 형식 오류 | `INVALID_TOKEN` | 401 |
| Blacklist 토큰 재사용 | `UNAUTHORIZED` | 401 |
| 탈퇴 처리된 계정 | `FORBIDDEN` | 403 |

---

## 테스트 전략

| 레이어 | 대상 | 방식 |
|--------|------|------|
| Domain | User / SocialAccount 생성 및 상태 검증 | 단위 테스트 |
| Application | SocialLoginUseCase (신규/재방문/연동) | MockK (Port mock) |
| Application | TokenRefreshUseCase, LogoutUseCase, WithdrawUseCase | MockK |
| Infrastructure | JwtProvider 발급/검증/만료 | 단위 테스트 |
| Interfaces | AuthController 엔드포인트 전체 | `@WebMvcTest` + MockK |

---

## 향후 고려: 비회원 체험 기능

이번 Phase 미구현. 추후 아래 중 택일:
- **A안:** `UserRole.GUEST` 추가 + `GuestTokenUseCase` 신규 작성
- **B안:** auth 모듈 무관, `module-resume`에서 Redis 핑거프린트로 독립 처리
