# Phase 2 — Auth 모듈 트러블슈팅 기록

> 작성일: 2026-05-03
> 작업 범위: module-auth 구현 완료 → 전체 테스트 통과 → PR 생성

---

## 목차

1. [UseCase 빈 등록 누락 — NoSuchBeanDefinitionException](#1-usecase-빈-등록-누락--nosuchbeandefinitionexception)
2. [Kotlin final 클래스 + CGLIB 프록시 충돌 — AopConfigException](#2-kotlin-final-클래스--cglib-프록시-충돌--aopconfigexception)
3. [app 통합 테스트 환경 설정 누락 — PlaceholderResolutionException](#3-app-통합-테스트-환경-설정-누락--placeholderresolutionexception)

---

## 1. UseCase 빈 등록 누락 — NoSuchBeanDefinitionException

### 문제

```
Error creating bean with name 'customOAuth2UserService':
  Unsatisfied dependency — No qualifying bean of type
  'com.atomiccv.auth.application.usecase.OAuthLoginUseCase' available
```

`./gradlew test` 시 `AtomicCvApplicationTests.contextLoads()` 실패.

### 원인

DDD Hexagonal 아키텍처 원칙에 따라 `application` 레이어는 Spring 의존성을 갖지 않도록 설계했다.
그 결과 `OAuthLoginUseCase`, `TokenRefreshUseCase`, `LogoutUseCase` 모두 `@Service` / `@Component` 어노테이션이 없어 Spring 컴포넌트 스캔에서 제외되었다.

```
의존성 체인:
SecurityConfig → CustomOAuth2UserService → OAuthLoginUseCase (빈 없음)
```

### 해결

`infrastructure` 레이어에 `@Configuration` 클래스를 추가해 UseCase를 `@Bean` 팩토리 메서드로 명시 등록했다.

```kotlin
// module-auth/src/main/kotlin/com/atomiccv/auth/infrastructure/AuthConfiguration.kt
@Configuration
class AuthConfiguration {
    @Bean
    fun oAuthLoginUseCase(
        userRepository: UserRepository,
        socialAccountRepository: SocialAccountRepository,
        jwtPort: JwtPort,
        refreshTokenPort: RefreshTokenPort,
    ): OAuthLoginUseCase =
        OAuthLoginUseCase(
            userRepository = userRepository,
            socialAccountRepository = socialAccountRepository,
            jwtPort = jwtPort,
            refreshTokenPort = refreshTokenPort,
        )

    @Bean
    fun tokenRefreshUseCase(refreshTokenPort: RefreshTokenPort, jwtPort: JwtPort) =
        TokenRefreshUseCase(refreshTokenPort = refreshTokenPort, jwtPort = jwtPort)

    @Bean
    fun logoutUseCase(jwtPort: JwtPort, tokenBlacklistPort: TokenBlacklistPort, refreshTokenPort: RefreshTokenPort) =
        LogoutUseCase(jwtPort = jwtPort, tokenBlacklistPort = tokenBlacklistPort, refreshTokenPort = refreshTokenPort)
}
```

### 핵심 원칙

| 방법 | Spring 의존성 | 빈 등록 방식 |
|------|-------------|------------|
| `@Service` 추가 | application 레이어에 Spring 침투 | 자동 컴포넌트 스캔 |
| `@Configuration` + `@Bean` | infrastructure 레이어에서 격리 처리 | 명시 팩토리 등록 ✅ |

DDD에서 application 레이어는 순수 비즈니스 로직만 가져야 한다.
Spring 의존성은 infrastructure 레이어(`AuthConfiguration`)가 전담한다.

---

## 2. Kotlin final 클래스 + CGLIB 프록시 충돌 — AopConfigException

### 문제

```
AopConfigException: Could not generate CGLIB subclass of class
  com.atomiccv.auth.application.usecase.OAuthLoginUseCase
Caused by: IllegalArgumentException: Cannot subclass final class OAuthLoginUseCase
```

`AuthConfiguration`으로 UseCase 빈 등록 문제를 해결하자 새로운 에러 발생.

### 원인

**Kotlin 클래스는 기본적으로 `final`이다.**

Spring은 `@Transactional`이 선언된 메서드에 AOP 트랜잭션 프록시를 적용한다.
이 때 CGLIB이 해당 클래스의 서브클래스를 런타임에 생성하는데, 클래스가 `final`이면 상속이 불가능하다.

```bash
# 실제 컴파일된 바이트코드 확인
javap -v OAuthLoginUseCase.class | grep "flags:"
# → flags: (0x0031) ACC_PUBLIC, ACC_FINAL, ACC_SUPER  ← final!
```

**왜 `kotlin("plugin.spring")`이 해결해주지 않았는가?**

`plugin.spring`은 클래스 레벨에 Spring 어노테이션(`@Component`, `@Transactional` 등)이 있을 때 자동으로 `open`을 적용한다.
`OAuthLoginUseCase`는 `@Transactional`이 메서드 레벨에만 있고 클래스 레벨에는 없었으므로, 플러그인이 해당 클래스를 `open`으로 처리하지 않았다.

```kotlin
// 기존 — 메서드 레벨만 → 클래스는 final 유지
class OAuthLoginUseCase(...) {
    @Transactional          // ← 메서드 레벨
    fun login(...): TokenResult { ... }
}
```

### 해결

`@Transactional`을 클래스 레벨로 이동했다.
`plugin.spring`이 클래스 레벨 어노테이션을 감지해 컴파일 시 `open`을 자동 적용한다.

```kotlin
@Transactional              // ← 클래스 레벨로 이동
class OAuthLoginUseCase(...) {
    fun login(...): TokenResult { ... }
}
```

```bash
# 수정 후 재컴파일 확인
javap -v OAuthLoginUseCase.class | grep "flags:"
# → flags: (0x0021) ACC_PUBLIC, ACC_SUPER  ← final 사라짐 ✅
```

### plugin.spring 동작 원리

| 어노테이션 위치 | plugin.spring 반응 | 결과 |
|-------------|------------------|------|
| 클래스 레벨에 `@Transactional` | 클래스 + 전체 멤버를 `open`으로 처리 | CGLIB 프록시 가능 ✅ |
| 메서드 레벨만 `@Transactional` | 클래스는 그대로 `final` | CGLIB 프록시 실패 ❌ |

---

## 3. app 통합 테스트 환경 설정 누락 — PlaceholderResolutionException

### 문제

```
PlaceholderResolutionException: Could not resolve placeholder 'jwt.secret'
  in value "${jwt.secret}"
```

`AuthConfiguration` 빈 등록 + CGLIB 문제를 해결하자 세 번째 에러 발생.

### 원인

`app` 모듈의 통합 테스트(`AtomicCvApplicationTests`)가 `module-auth`의 빈을 로드하면서
`JwtProvider`가 `jwt.secret` 프로퍼티를 요구했다.

`app/src/test/resources/application-test.yaml`에는 H2 DB 설정만 있었고
JWT, OAuth2, Redis 설정이 모두 누락된 상태였다.

또한 `StringRedisTemplate` 빈이 없어 `RefreshTokenRedisAdapter` 초기화도 실패했다.
이는 `RedisAutoConfiguration`을 테스트 exclude 목록에 포함시켰기 때문이다.

```yaml
# 기존 app/src/test/resources/application-test.yaml — 설정 부족
spring:
  autoconfigure:
    exclude:
      - RedisAutoConfiguration          # ← StringRedisTemplate 빈도 제외됨
      - RedisRepositoriesAutoConfiguration
```

### 해결

`application-test.yaml`에 누락된 설정을 추가했다.
`RedisAutoConfiguration`은 exclude 목록에서 제거해 `StringRedisTemplate` 빈 생성을 허용했다.
(`RedisRepositoriesAutoConfiguration`은 레포지토리 스캔을 막기 위해 제외 유지)

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
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

### 참고: RedisAutoConfiguration vs RedisRepositoriesAutoConfiguration

| AutoConfiguration | 역할 | 테스트 시 포함 여부 |
|------------------|------|----------------|
| `RedisAutoConfiguration` | `StringRedisTemplate`, `RedisTemplate` 빈 등록 | **포함** (어댑터가 의존) |
| `RedisRepositoriesAutoConfiguration` | `@RedisHash` 레포지토리 스캔 | **제외** (사용 안 함) |

---

## 최종 결과

| 항목 | 결과 |
|------|------|
| module-auth 단위 테스트 | ✅ 전체 통과 (UseCase 3개, Controller) |
| AtomicCvApplicationTests.contextLoads() | ✅ 통과 |
| ktlint 검사 | ✅ 통과 |
| PR 생성 | ✅ [resume-helper/BE#1](https://github.com/resume-helper/BE/pull/1) |
