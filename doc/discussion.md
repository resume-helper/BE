# Atomic CV — 백엔드 논의 사항 리스트

> 작성일: 2026-04-30
> 목적: 팀 회의 전 사전 정리 / 하나씩 확정해 나가는 추적 문서
> 상태 표기: ✅ 확정 / ❓ 논의 필요 / 🔜 나중에 결정

---

## 목차

1. [즉시 결정 필요 (개발 착수 블로커)](#1-즉시-결정-필요-개발-착수-블로커)
2. [Step 1~2 전 결정 필요 (환경 셋팅 블로커)](#2-step-12-전-결정-필요-환경-셋팅-블로커)
3. [Step 2~3 전 결정 필요 (구현 설계 블로커)](#3-step-23-전-결정-필요-구현-설계-블로커)
4. [코드 컨벤션](#4-코드-컨벤션)
5. [Phase 2 이후 결정 가능](#5-phase-2-이후-결정-가능)

---

## 1. 즉시 결정 필요 (개발 착수 블로커)

---

### ✅ [1] 이메일 인증 도입 여부

| 항목 | 내용 |
|------|------|
| **결정** | 미사용 — 소셜 로그인 전용으로 결정 |
| **이유** | 일반 로그인(이메일+비밀번호) 미구현으로 이메일 인증 불필요 |
| **결정일** | 2026-05-02 |

---

### ❓ [2] ERD 설계 확정

| 항목 | 내용 |
|------|------|
| **현황** | `docs/ERD_DRAFT.md` 초안 작성 완료 |
| **논의 필요** | 아래 4가지 설계 결정 포인트 |
| **결정 기한** | 금일 회의 |

**논의 포인트**:
1. 블록 저장 단위 — 전체 일괄 저장 vs 블록 단위 개별 저장 (선행 결정 필요, 팀 추가 논의 예정)
2. 블록 버전 관리 전략 — `snapshot_data` JSON vs `block_versions` 별도 테이블 (1번 결정 후 확정 가능)
3. `resume_blocks` 테이블 필요 여부
4. `reviewer_ip` 원문 저장 vs SHA-256 해싱
5. `notifications` 테이블 위치 — Feedback Context vs shared 모듈

---

### ✅ [3] API 응답 포맷 & 예외 처리 구조

| 항목 | 내용 |
|------|------|
| **결정** | 공통 래퍼(선택지 B) 사용 — `ApiResponse<T>` |
| **상세 명세** | `doc/API_RESPONSE.md` |
| **결정일** | 2026-05-01 |

**논의 포인트**:

**1) 성공 응답 포맷 — 두 가지 선택지**

```json
// 선택지 A: HTTP 상태코드에 의존
// 200 OK
{ "id": 1, "title": "카카오 지원 이력서", "slug": "abc-123" }

// 선택지 B: 공통 래퍼 사용
// 200 OK
{
  "success": true,
  "data": { "id": 1, "title": "카카오 지원 이력서", "slug": "abc-123" },
  "message": null
}
```

**2) 에러 응답 포맷 (초안)**

```json
// 예: 400 Bad Request
{
  "code": "VALIDATION_FAILED",
  "message": "이메일 형식이 올바르지 않습니다.",
  "errors": [
    { "field": "email", "message": "올바른 이메일 형식을 입력해주세요." }
  ],
  "timestamp": "2026-05-01T09:30:00"
}
```

**3) 비즈니스 에러코드 체계 (초안)**

| HTTP 상태 | 에러 코드 | 설명 |
|----------|----------|------|
| 400 | `VALIDATION_FAILED` | 입력값 유효성 검증 실패 |
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `FORBIDDEN` | 권한 없음 (타인 리소스 접근 등) |
| 404 | `RESOURCE_NOT_FOUND` | 리소스 없음 |
| 409 | `DUPLICATE_EMAIL` | 이메일 중복 가입 |
| 429 | `RATE_LIMIT_EXCEEDED` | Rate Limit 초과 (피드백 어뷰징 등) |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 2. Step 1~2 전 결정 필요 (환경 셋팅 블로커)

---

### ✅ [4] Spring Boot 버전 확정

| 항목 | 내용 |
|------|------|
| **결정** | Spring Boot 3.x 유지 |
| **이유** | Kotlin이 Spring Boot 4.x와 호환되지 않음 |
| **결정일** | 2026-04-30 |

---

### ✅ [5] 로컬 개발 환경 통일 방안

| 항목 | 내용 |
|------|------|
| **결정** | DB: 기존 생성된 RDB(AWS) 연결 / Redis: 로컬 구동 |
| **이유** | AWS Redis OSS는 VPC 문제로 로컬에서 직접 연결 불가 |
| **결정일** | 2026-04-30 |

---

### ✅ [6] 브랜치 전략 & PR 머지 룰

| 항목 | 내용 |
|------|------|
| **결정** | MVP 단계 간소화 전략 (develop 브랜치 없음) |
| **결정일** | 2026-04-30 |

**확정 브랜치 전략**:
```
main          ← 프로덕션 배포 브랜치 (직접 push 금지)
  └─ feature/{기능명}  ← 기능 단위 개발
  └─ fix/{버그명}      ← 버그 수정
  └─ chore/{작업명}    ← 설정·의존성 변경
```

**확정 PR 룰**:
- PR 머지 방식: Merge
- 승인자: AI (skill 파일 생성 예정)
- PR 템플릿: `docs/pipeline/pr-template.md` 사용

---

### ✅ [7] CLAUDE.md 세부 규칙 범위

| 항목 | 내용 |
|------|------|
| **결정** | 권장(B) 범위로 시작, 필요 시 실시간 업데이트 |
| **포함 내용** | 프로젝트 개요, 패키지 컨벤션, API 응답 포맷, PR 체크리스트, 코드 스타일, 테스트 작성 규칙 |
| **결정일** | 2026-04-30 |

---

## 3. Step 2~3 전 결정 필요 (구현 설계 블로커)

---

### ✅ [8] Bounded Context 간 데이터 공유 방식

| 항목 | 내용 |
|------|------|
| **결정** | 초안 그대로 확정, 이후 필요 시 수정 |
| **결정일** | 2026-04-30 |

**확정 매핑**:

| 출발 | 도착 | 패턴 | 확정 여부 |
|------|------|------|---------|
| resume → block | 블록 데이터 조회 | 패턴 A (Port) | ✅ |
| resume → analytics | 발행 후 세션 생성 | 패턴 B (Event) | ✅ |
| resume → feedback | 발행 후 슬롯 초기화 | 패턴 B (Event) | ✅ |
| feedback → resume | 피드백 집계 조회 | 패턴 A (Port) | ✅ |

---

### ✅ [9] 블록 버전 관리 전략

| 항목 | 내용 |
|------|------|
| **결정** | block_versions 별도 테이블 (B) |
| **리스크 완화** | 발행 시점 스냅샷 JSON 병행 저장(복원 캐시) / 최근 N개 버전만 보관 / 발행 트랜잭션 내 일괄 insert |
| **결정일** | 2026-04-30 |

---

### ❓ [10] 웹 이력서 슬러그 구조

| 항목 | 내용 |
|------|------|
| **결정 기한** | Step 4 시작 전 |

| 선택지 | 예시 | 비고 |
|--------|------|------|
| **UUID 자동 생성** (초안) | `/r/550e8400-e29b` | 간단, 충돌 없음 |
| **Short UUID** | `/r/aB3kX9` | 짧고 공유하기 쉬움 |
| **사용자 지정 slug 허용** | `/r/my-resume` | UX 좋음, 중복 검사 필요 |

---

### ✅ [11] Redis HA 구성 여부

| 항목 | 내용 |
|------|------|
| **결정** | 단일 Redis (Single) |
| **이유** | 현재 JWT Refresh Token 관리 외 사용처 없음, MVP 단계에서 HA 불필요 |
| **운영 전략** | 장애 발생 시 빠른 재시작으로 대응 |
| **결정일** | 2026-04-30 |

---

## 4. 코드 컨벤션

> 팀원 전원이 동의한 뒤 `CLAUDE.md`의 `rules/` 섹션에 공식 등록

---

### ✅ [12] 코드 컨벤션 확정

#### 4-1. 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| **패키지** | 소문자, 점(.) 구분 | `com.atomiccv.resume.application` |
| **클래스** | PascalCase | `ResumePublishUseCase` |
| **함수 / 변수** | camelCase | `publishResume()`, `resumeId` |
| **상수** | UPPER_SNAKE_CASE | `MAX_FILE_SIZE_MB` |
| **Enum 값** | UPPER_SNAKE_CASE | `GOOGLE`, `KAKAO` |
| **DB 테이블** | snake_case, 복수형 | `resume_versions`, `feedback_tags` |
| **DB 컬럼** | snake_case | `created_at`, `user_id` |

#### 4-2. 클래스 Suffix 규칙

| 레이어 | Suffix | 예시 |
|--------|--------|------|
| Aggregate Root | 없음 | `Resume`, `Block`, `User` |
| Use Case | `UseCase` | `PublishResumeUseCase` |
| Domain Service | `DomainService` | `AuthDomainService` |
| Repository Interface | `Repository` | `ResumeRepository` |
| JPA Base Entity | `BaseJpaEntity` (module-shared 상속) | `BaseJpaEntity` |
| JPA Repository | `JpaRepository` | `ResumeJpaRepository` |
| Repository Impl | `RepositoryImpl` | `ResumeRepositoryImpl` |
| REST Controller | `Controller` | `ResumeController` |
| Request DTO | `Request` | `PublishResumeRequest` |
| Response DTO | `Response` | `ResumeDetailResponse` |
| Application Command | `Command` | `PublishResumeCommand` |
| Application Query | `Query` | `GetResumeQuery` |
| Event | `Event` | `ResumePublishedEvent` |
| Event Handler | `EventHandler` | `ResumePublishedEventHandler` |
| Port Interface | `Port` | `BlockQueryPort` |
| Port Adapter | `Adapter` | `BlockQueryAdapter` |
| External Client | `Client` | `GoogleOAuthClient` |
| Config 클래스 | `Config` | `SecurityConfig`, `JpaConfig` |
| Exception | `Exception` | `ResumeNotFoundException` |

#### 4-3. 파일 / 패키지 구조 규칙

```
// ✅ 올바른 구조 — 도메인 내 레이어 분리
com.atomiccv.resume.domain.model.Resume
com.atomiccv.resume.application.usecase.PublishResumeUseCase
com.atomiccv.resume.infrastructure.persistence.ResumeRepositoryImpl
com.atomiccv.resume.interfaces.rest.ResumeController

// ❌ 잘못된 구조 — 레이어로 묶기
com.atomiccv.controller.ResumeController    // 도메인 경계 없음
com.atomiccv.service.ResumeService          // 도메인 경계 없음
```

#### 4-4. 의존성 방향 규칙

```
interfaces → application → domain
infrastructure → domain
```

- `domain`은 어떤 외부 라이브러리도 import하지 않는다 (순수 Kotlin)
- `application`은 `domain`만 참조한다 (JPA, Spring 등 import 금지)
- `interfaces`는 `application`만 참조한다 (`domain` 직접 참조 금지)
- 모듈 간 직접 참조 금지 — Port Interface 또는 Event를 통해 통신

#### 4-5. 예외 처리 규칙

```kotlin
// ✅ 도메인 예외는 domain 레이어에 정의
class ResumeNotFoundException(resumeId: Long) :
    BusinessException("RESOURCE_NOT_FOUND", "이력서를 찾을 수 없습니다. id=$resumeId")

// ✅ 공통 비즈니스 예외 base class (shared 모듈)
open class BusinessException(
    val code: String,
    override val message: String
) : RuntimeException(message)

// ✅ 전역 예외 핸들러 (interfaces 레이어)
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> { ... }
}
```

#### 4-6. 트랜잭션 규칙

```kotlin
// ✅ 트랜잭션 경계는 application 레이어 UseCase에만 선언
// UseCase는 Spring 어노테이션 없이 순수 Kotlin으로 작성
@Transactional
class PublishResumeUseCase(...) { ... }

// ✅ UseCase 빈 등록은 infrastructure 레이어의 @Configuration 클래스에서 담당
// (DDD 원칙: application 레이어는 Spring 의존성 없음)
@Configuration
class ResumeConfiguration {
    @Bean
    fun publishResumeUseCase(...): PublishResumeUseCase = PublishResumeUseCase(...)
}

// ❌ UseCase에 @Service 금지 — application 레이어에 Spring 침투
// ❌ domain Service에 @Transactional 금지 (인프라 의존)
// ❌ Controller에 @Transactional 금지
```

#### 4-7. 테스트 네이밍 규칙

```kotlin
// ✅ 테스트 메서드명: 한글 허용, 행위_조건_결과 형식
@Test
fun `이력서 발행 시 slug가 생성되고 버전 스냅샷이 저장된다`() { ... }

@Test
fun `존재하지 않는 이력서 발행 시 ResumeNotFoundException이 발생한다`() { ... }
```

#### 4-8. 코드 스타일 도구

| 도구 | 설정 | 설명 |
|------|------|------|
| **ktlint** | `.editorconfig` | Kotlin 코드 스타일 자동 검사 |
| **detekt** | `detekt.yml` | 정적 분석 (복잡도, 코드 smell 검사) |
| **Gradle pre-commit hook** | `hooks/pre-commit` | 커밋 전 ktlint 자동 실행 |

> ktlint, detekt 설정 파일은 Step 1 환경 셋팅 시 함께 추가

#### 4-9. 주석 규칙

```kotlin
// ✅ 허용 — 복잡한 비즈니스 로직 설명
// Refresh Token 만료 시간은 Access Token보다 7배 길게 설정 (보안 정책)
val refreshExpiry = accessExpiry * 7

// ✅ 허용 — TODO 형식
// TODO: Phase 2에서 Claude AI 분석으로 대체
val score = calculateManually()

// ❌ 금지 — 코드 자체를 설명하는 주석 (코드로 표현 가능한 내용)
// 유저를 찾는다
val user = userRepository.findById(userId)
```

---

## 5. Phase 2 이후 결정 가능

---

### 🔜 [13] 모니터링 스택 선택

| 항목 | 내용 |
|------|------|
| **현황** | 미정 (고도화 단계에서 결정) |
| **선택지** | ELK Stack vs Prometheus + Grafana |

---

### 🔜 [14] WebFlux 전환 시점 기준

| 항목 | 내용 |
|------|------|
| **현황** | MVP는 MVC + Coroutine 조합 사용 |
| **트리거** | AI 연동 (Phase 2) 또는 동시 접속자 급증 시 |

---

### 🔜 [15] MSA 전환 트리거 기준

| 항목 | 내용 |
|------|------|
| **현황** | Modular Monolith 유지 |
| **논의 필요 기준 (초안)** | 일 트래픽 10만 요청 초과 또는 AI·결제 모듈 팀 분리 시 |

---

### ❓ [16] 작업 분배 확정

| 항목 | 내용 |
|------|------|
| **현황** | 준영님: 평일 9~18시, 야간 9시~ |
| **방법** | PRD 기능 명세 작성 완료 후 분담 협의 예정 |
| **결정 기한** | PRD 기능 명세 완료 후 |

---

### ❓ [17] module-feedback / module-analytics 분리 여부

| 항목 | 내용 |
|------|------|
| **현황** | 초안: 두 모듈 분리. 재검토 필요 |
| **결정 기한** | 멀티모듈 구조 설정 착수 전 (TASKS.md 1-2-2) |

**선택지**:
- A. 분리 유지 (`module-feedback` + `module-analytics`)
- B. `module-engagement`로 통합 — 두 기능 모두 `ResumePublishedEvent` 기반, MVP 규모에 적합
- C. `module-resume`에 흡수 — 모듈 최소화, 단 resume 모듈 비대화 우려

**배경**: 두 모듈 모두 이력서 발행 이후 동작. B가 응집도 자연스러우나 팀 합의 필요.

---

## 확정 사항 요약

| # | 항목 | 결정 내용 | 결정일 |
|---|------|----------|--------|
| 1 | 이메일 인증 | 미사용 (소셜 로그인 전용) | 2026-05-02 |
| 3 | API 응답 포맷 | 공통 래퍼 `ApiResponse<T>` 사용, 상세: `doc/API_RESPONSE.md` | 2026-05-01 |
| 4 | Spring Boot 버전 | 3.x 유지 (Kotlin 4.x 미호환) | 2026-04-30 |
| 5 | 로컬 개발 환경 | DB: AWS RDB 연결 / Redis: 로컬 구동 | 2026-04-30 |
| 6 | 브랜치 전략 | main + feature/fix/chore, PR: Merge / AI 승인 / 템플릿 사용 | 2026-04-30 |
| 7 | CLAUDE.md 범위 | 권장(B) 범위로 시작, 실시간 업데이트 | 2026-04-30 |
| 8 | Bounded Context 패턴 | Port(조회) / Event(발행 후 처리) 초안 확정 | 2026-04-30 |
| 9 | 블록 버전 관리 | block_versions 별도 테이블 + 스냅샷 JSON 병행 | 2026-04-30 |
| 11 | Redis HA | 단일 Redis (JWT Refresh Token 관리 전용) | 2026-04-30 |
| 12 | 코드 컨벤션 | 초안 전체 확정 (네이밍/Suffix/패키지/의존성/예외/트랜잭션/테스트/도구/주석) | 2026-04-30 |
| — | 인증 방식 | JWT + Redis Blacklist + HttpOnly Cookie 전달 (ADR-01) | 2026-04-29 |
| — | 비동기 처리 | Kotlin Coroutine 우선 (ADR-02) | 2026-04-29 |
| — | PDF 생성 | FE 전담 (ADR-03) | 2026-04-29 |
| — | 아키텍처 | Full Monolith → Modular Monolith (ADR-04) | 2026-04-29 |

---

> 항목 확정 시 상태를 `❓ → ✅`로 변경하고 결정 내용 및 날짜를 기록해 주세요.
