# Atomic CV — 멀티모듈 구조 설계

> 작성일: 2026-05-01
> 상태: 초안 (일부 항목 팀 결정 대기)

---

## 모듈 구성

```
:app                  # Spring Boot 진입점, 모듈 조합, 전역 설정
:module-shared        # 공통 예외, ApiResponse, 유틸리티
:module-auth          # 인증 (OAuth2, JWT, Refresh Token)
:module-resume        # 이력서, 블록, 버전 관리
:module-feedback      # 피드백, Rate Limit          ⏸ [17] 팀 결정 후 확정
:module-analytics     # 조회 세션, 통계              ⏸ [17] 팀 결정 후 확정
```

> [17] `module-feedback` / `module-analytics` 분리 vs `module-engagement` 통합 여부는  
> `doc/discussion.md` [17] 참고. 멀티모듈 착수 전 팀 결정 필요.

---

## 모듈 간 의존성

```
app → module-auth, module-resume, module-feedback, module-analytics, module-shared
module-auth     → module-shared
module-resume   → module-shared
module-feedback → module-shared
module-analytics → module-shared
```

**규칙:** 모듈 간 직접 의존 금지. 통신은 Port Interface 또는 Domain Event만 허용.

---

## 각 모듈 내부 패키지 구조

모든 도메인 모듈은 Hexagonal Architecture를 따른다.

```
module-{domain}/
└── src/
    ├── main/kotlin/com/atomiccv/{domain}/
    │   ├── domain/
    │   │   ├── model/          # 엔티티, 값 객체 (순수 Kotlin, 외부 라이브러리 import 금지)
    │   │   └── repository/     # Repository 인터페이스
    │   ├── application/
    │   │   ├── usecase/        # UseCase 클래스 (트랜잭션 경계)
    │   │   ├── port/           # 모듈 간 통신용 Port 인터페이스
    │   │   └── event/          # 도메인 이벤트 정의
    │   ├── infrastructure/
    │   │   ├── persistence/    # JPA Entity, RepositoryImpl
    │   │   └── client/         # 외부 API 클라이언트
    │   └── interfaces/
    │       └── rest/           # Controller, Request/Response DTO
    └── test/kotlin/com/atomiccv/{domain}/
        ├── domain/             # 도메인 단위 테스트
        ├── application/        # UseCase 통합 테스트
        └── interfaces/         # Controller 슬라이스 테스트
```

---

## 모듈별 역할 요약

### :module-shared

| 패키지 | 내용 |
|--------|------|
| `common.response` | `ApiResponse<T>` 공통 래퍼 |
| `common.exception` | `BusinessException` base, 에러코드 enum |
| `common.util` | 공통 유틸리티 |

> 다른 모듈이 공통으로 의존하는 라이브러리만 포함. 비즈니스 로직 포함 금지.

### :module-auth

| 담당 기능 | 상세 |
|-----------|------|
| OAuth2 소셜 로그인 | Google, Kakao |
| JWT 발급 / 검증 | Access Token, Refresh Token |
| Redis Blacklist | 로그아웃 처리 |
| 이메일 인증 | Gmail SMTP (JavaMailSender) |

### :module-resume

| 담당 기능 | 상세 |
|-----------|------|
| 이력서 CRUD | 생성 / 조회 / 수정 / 삭제 |
| 블록 관리 | 블록 CRUD, 순서 변경, 버전 이력 |
| 발행 | 슬러그 생성, 버전 스냅샷 저장 |
| 이벤트 발행 | `ResumePublishedEvent` → feedback, analytics 수신 |

### :module-feedback ⏸

| 담당 기능 | 상세 |
|-----------|------|
| 피드백 슬롯 | 발행 이벤트 수신 후 슬롯 초기화 |
| 피드백 CRUD | 요청 생성, 등록, 집계 조회 |
| Rate Limit | Redis 기반 어뷰징 방지 |

### :module-analytics ⏸

| 담당 기능 | 상세 |
|-----------|------|
| 조회 세션 | 발행 이벤트 수신 후 세션 생성 |
| 통계 | 조회수, 방문자 집계 API |

---

## 모듈 간 통신 패턴

| 출발 → 도착 | 패턴 | 예시 |
|------------|------|------|
| resume → block (내부) | 직접 참조 (같은 모듈) | — |
| resume → feedback | Domain Event | `ResumePublishedEvent` |
| resume → analytics | Domain Event | `ResumePublishedEvent` |
| feedback → resume | Port Interface | `ResumeQueryPort` |

> 상세: `doc/discussion.md` [8] Bounded Context 간 데이터 공유 방식

---

## settings.gradle.kts 구조 (예시)

```kotlin
rootProject.name = "atomic-cv"

include(
    ":app",
    ":module-shared",
    ":module-auth",
    ":module-resume",
    ":module-feedback",   // ⏸ [17] 팀 결정 후 확정
    ":module-analytics",  // ⏸ [17] 팀 결정 후 확정
)
```
