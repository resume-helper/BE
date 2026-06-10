# Social Login API 설계

> 작성일: 2026-06-11
> 작업 범위: `module-auth` — `POST /api/auth/social-login` 신규 엔드포인트
> 관련 모듈 문서: `module-auth/CLAUDE.md`, `doc/SERVICE_POLICY.md`, `doc/API_RESPONSE.md`

---

## 1. 배경 및 목적

### 변경 배경

기존 OAuth2 흐름은 백엔드(Spring Security OAuth2 Client)가 Google·Kakao·Naver와 직접 통신했다. 이제 **FE(NextAuth.js)가 소셜 로그인을 수행**하고, BE는 **사용자 식별 정보를 받아 토큰만 발급**하는 구조로 전환한다.

### 이 endpoint의 책임

- FE로부터 받은 소셜 사용자 정보 검증 (현재: 기본 정합성, 향후: ID Token 서명)
- 기존 사용자 조회 (SocialAccount 또는 email 기준)
- 신규 사용자면 자동 가입
- JWT access_token + refresh_token 발급 후 쿠키 세팅

### 정책 보존

`doc/SERVICE_POLICY.md`의 인증·계정 연동 정책을 그대로 유지한다:
- 한 유저가 여러 소셜 계정 동시 연동 가능
- 동일 이메일 + 다른 provider → 같은 user에 social_account 추가
- 다른 이메일 → 별도 user

이 동작은 기존 `OAuthLoginUseCase`에 이미 구현되어 있어 그대로 재사용한다.

---

## 2. API 명세

### Endpoint

```
POST /api/auth/social-login
Content-Type: application/json
Origin: <FE origin>
```

### Request Body

```json
{
  "provider": "GOOGLE",
  "providerUserId": "108472937562834759283",
  "email": "user@example.com",
  "name": "홍길동"
}
```

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `provider` | String | ✓ | `GOOGLE` / `KAKAO` / `NAVER` (대문자) |
| `providerUserId` | String | ✓ | 소셜 제공자의 사용자 고유 ID |
| `email` | String | ✓ | 사용자 이메일 |
| `name` | String | ✓ | 사용자 이름 |

`profileImageUrl`은 페이로드에 포함하지 않는다 (이후 사용자 마이페이지에서 업로드).

### Response (성공)

```http
HTTP/1.1 200 OK
Set-Cookie: access_token=<JWT>; HttpOnly; Secure; SameSite=<env>; Domain=<env>; Path=/; Max-Age=3600
Set-Cookie: refresh_token=<UUID>; HttpOnly; Secure; SameSite=<env>; Domain=<env>; Path=/api/auth/refresh; Max-Age=604800
Content-Type: application/json

{"success": true, "data": null, "message": null}
```

쿠키 속성은 기존 `OAuth2AuthenticationSuccessHandler`와 동일 (`app.cookie-same-site`, `app.cookie-domain` 환경변수 사용).

### Response (에러)

`doc/API_RESPONSE.md` 컨벤션 준수.

| 케이스 | code | HTTP |
|---|---|---|
| provider 값이 enum에 없음 | `VALIDATION_FAILED` | 400 |
| email/name/providerUserId 빈 값 | `VALIDATION_FAILED` | 400 |
| 탈퇴 사용자 (grace period 만료) | `FORBIDDEN` | 403 |

```json
{"code": "VALIDATION_FAILED", "message": "지원하지 않는 provider입니다: XYZ"}
```

---

## 3. 컴포넌트 구조

DDD Hexagonal 패턴 준수 — `application` 레이어는 Spring 의존성 없음, 빈 등록은 `infrastructure/AuthConfiguration.kt`가 전담 (`doc/phases/phase-2-core/auth/TROUBLESHOOTING.md` 정책).

### 신규 파일

```
module-auth/src/main/kotlin/com/atomiccv/auth/
├── application/usecase/
│   └── SocialLoginUseCase.kt          ← 신규 (Spring 의존성 없음)
└── interfaces/rest/dto/
    └── SocialLoginRequest.kt          ← 신규 (Request DTO)
```

### 수정 파일

```
├── infrastructure/AuthConfiguration.kt ← @Bean 팩토리 추가
├── interfaces/rest/AuthController.kt   ← socialLogin() 메서드 추가
└── infrastructure/SecurityConfig.kt    ← /api/auth/social-login permitAll
```

### 책임 분리

| 컴포넌트 | 책임 |
|---|---|
| `AuthController.socialLogin()` | HTTP I/O, Request DTO 수신, 쿠키 세팅, `ApiResponse` 반환 |
| `SocialLoginUseCase` | 입력 정합성 검증, provider 문자열 → enum 변환, (향후) ID Token 검증, `OAuthLoginCommand` 변환 후 위임 |
| `OAuthLoginUseCase` | 기존 그대로 — 사용자 조회/생성/연동, 토큰 발급 |
| `AuthConfiguration` | `SocialLoginUseCase` `@Bean` 등록 |

---

## 4. 데이터 흐름

```
POST /api/auth/social-login
  │
  ▼
AuthController.socialLogin(request)
  │ - SocialLoginRequest 수신
  │ - socialLoginUseCase.login(request.toCommand())
  ▼
SocialLoginUseCase.login(SocialLoginCommand)
  │ - provider 문자열 → SocialProvider enum 변환
  │   ├─ 변환 실패 → BusinessException(VALIDATION_FAILED)
  │ - email/name/providerUserId 빈 값 검증
  │   ├─ 빈 값 → BusinessException(VALIDATION_FAILED)
  │ - (향후) ID Token 서명 검증 자리
  │ - OAuthLoginCommand로 변환 (profileImageUrl = null)
  │ - oAuthLoginUseCase.login(oAuthCommand)
  ▼
OAuthLoginUseCase.login() — 기존 로직 그대로
  │ - SocialAccount(provider + providerUserId)로 조회
  │   ├─ 있음 → User 반환 (grace period 처리 포함)
  │   └─ 없음 → email로 User 조회
  │     ├─ 있음 → 신규 SocialAccount 연동 후 User 반환
  │     └─ 없음 → 신규 User + SocialAccount 생성
  │ - JWT access_token 발급
  │ - UUID refresh_token 생성 → Redis 저장 (TTL 7일)
  │ - TokenResult(accessToken, refreshToken) 반환
  ▼
AuthController.socialLogin() (계속)
  │ - access_token 쿠키 세팅 (Path=/, MaxAge=1h)
  │ - refresh_token 쿠키 세팅 (Path=/api/auth/refresh, MaxAge=7d)
  │ - ApiResponse.ok() 응답
  ▼
HTTP 200 + 2x Set-Cookie
```

---

## 5. 보안

### 신뢰 경계 (Trust boundary) — 임시 상태

현재 단계에서는 FE가 보낸 `provider + providerUserId + email + name`을 **그대로 신뢰**한다. 다음 위협이 존재:

| 위협 | 시나리오 |
|---|---|
| Account takeover | 공격자가 타인의 email/providerUserId로 요청 → 로그인 성공 |
| User impersonation | 임의 user 가입 가능 |

→ **향후 ID Token 검증 추가 시 해결**. 현재 endpoint는 **개발·테스트 환경에서만 사용**한다는 가정.

향후 검증 추가 위치는 `SocialLoginUseCase.login()` 내부의 "(향후) ID Token 서명 검증 자리" 주석 지점.

### Spring Security

`SecurityConfig.kt`의 `permitAll` 목록에 `/api/auth/social-login` 추가. 본 endpoint는 인증 자체이므로 인증 불필요.

### 기존 OAuth2 코드 (별도 PR로 정리 예정 — 본 설계 범위 외)

NextAuth.js 전환이 완료되면 다음을 일괄 제거 (별도 PR):
- `SecurityConfig.kt`의 `oauth2Login {...}` 블록
- `infrastructure/client/CustomOAuth2*`, `OAuth2*UserInfo`, `OAuth2UserInfoFactory`, `OAuth2AuthenticationSuccessHandler`, `OAuth2AuthenticationFailureHandler`, `CustomOAuth2AuthorizationRequestResolver`
- `application-*.yml`의 `spring.security.oauth2.client.*`
- `module-auth/CLAUDE.md`의 OAuth2 관련 컴포넌트 기술

---

## 6. 테스트 전략

`module-auth/CLAUDE.md` 테스트 전략 준수.

### Application 레이어 — `SocialLoginUseCaseTest` (MockK)

| 시나리오 | 검증 |
|---|---|
| 정상 요청 (GOOGLE/KAKAO/NAVER) | `OAuthLoginUseCase.login()` 호출 시 올바른 `OAuthLoginCommand` 전달, `profileImageUrl = null` |
| provider="INVALID" | `BusinessException("VALIDATION_FAILED")` |
| provider 소문자 ("google") | `BusinessException("VALIDATION_FAILED")` — 대문자 강제 |
| email 빈 문자열 | `BusinessException("VALIDATION_FAILED")` |
| providerUserId 빈 문자열 | `BusinessException("VALIDATION_FAILED")` |
| name 빈 문자열 | `BusinessException("VALIDATION_FAILED")` |
| `OAuthLoginUseCase`가 throw | 예외 그대로 전파 |

### Interfaces 레이어 — `AuthControllerTest` (`@WebMvcTest` + MockK)

| 시나리오 | 검증 |
|---|---|
| POST /api/auth/social-login 정상 body | 200, 2x Set-Cookie (access_token Path=/, refresh_token Path=/api/auth/refresh), body=`ApiResponse.ok()` |
| Body 누락 또는 잘못된 JSON | 400 |
| UseCase가 BusinessException 던짐 | 적절한 HTTP 상태 (`GlobalExceptionHandler` 거쳐 변환) |
| Set-Cookie 속성 검증 | HttpOnly=true, Secure=true, SameSite (cookie-same-site 기본값) |

---

## 7. 명명 컨벤션

`CLAUDE.md` 클래스 Suffix 표 준수:
- UseCase: `SocialLoginUseCase`
- Command: `SocialLoginCommand`
- Request DTO: `SocialLoginRequest`
- Controller 메서드: `socialLogin` (camelCase)

패키지 위치도 기존 구조 그대로:
- UseCase → `application.usecase`
- Request DTO → `interfaces.rest.dto`
- Controller 메서드 → 기존 `AuthController`

---

## 8. 검증 및 완료 기준

| 항목 | 완료 기준 |
|---|---|
| 빌드 | `./gradlew :module-auth:compileKotlin` 통과 |
| 단위 테스트 | `SocialLoginUseCaseTest` 7개 시나리오 통과 |
| 통합 테스트 | `AuthControllerTest`의 social-login 시나리오 4개 통과 |
| Detekt | `./gradlew :module-auth:detekt` 통과 |
| 수동 검증 | curl로 POST 호출 → 200 + Set-Cookie 2개 응답 확인 |
| 회귀 | 기존 `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/me` 동작 변함 없음 |

---

## 9. 범위 외 (Out of scope)

- ID Token 서명 검증 — 향후 PR
- 기존 OAuth2 코드 제거 — 향후 PR
- `module-auth/CLAUDE.md` 갱신 — 향후 PR
- FE NextAuth.js 통합 — FE팀 작업
