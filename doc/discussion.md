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
| **결정** | Gmail SMTP (JavaMailSender) 사용 |
| **이유** | 비용 절감 우선 (AWS SES $0.10/1,000건 vs Gmail 무료), MVP 발송량 500건/일 한도 이내 예상 |
| **제약사항** | 발송량 500건/일 초과 시 AWS SES로 전환 필요 |
| **결정일** | 2026-04-30 |

---

### ❓ [2] ERD 설계 확정

| 항목 | 내용 |
|------|------|
| **현황** | `docs/ERD_DRAFT.md` 초안 작성 완료 |
| **논의 필요** | 아래 4가지 설계 결정 포인트 |
| **결정 기한** | 금일 회의 |

**논의 포인트**:
1. 블록 버전 관리 전략 — `snapshot_data` JSON vs `block_versions` 별도 테이블
2. `resume_blocks` 테이블 필요 여부
3. `reviewer_ip` 원문 저장 vs SHA-256 해싱
4. `notifications` 테이블 위치 — Feedback Context vs shared 모듈

---

### ❓ [3] API 응답 포맷 & 예외 처리 구조

| 항목 | 내용 |
|------|------|
| **목적** | FE·BE 간 계약, 에러 핸들링 일관성 확보 |
| **담당** | BE + FE 협의 |
| **결정 기한** | Step 2 시작 전 (5/8 이전) |

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

### ❓ [4] Spring Boot 버전 확정

| 항목 | 내용 |
|------|------|
| **현황** | 3.x 사용 중, 4.x 검토 중 |
| **결정 기한** | Step 1 시작 전 (5/1 이전) |

**비교**:

| 항목 | Spring Boot 3.x | Spring Boot 4.x |
|------|---------------|---------------|
| Java 최소 | Java 17 | Java 21 (예상) |
| 안정성 | 검증된 LTS | 출시 초기 |
| 팀 경험 | 있음 | 없음 |
| 권장 | ✅ MVP 단계 | Phase 2 이후 고려 |

---

### ❓ [5] 로컬 개발 환경 통일 방안

| 항목 | 내용 |
|------|------|
| **목적** | 팀원 간 환경 차이로 인한 "내 PC에선 되는데" 방지 |
| **결정 기한** | Step 1 시작 전 |

**선택지**:
- A) `docker-compose.yml`로 MySQL + Redis 로컬 구동 통일 ✅ (권장)
- B) 개별 설치 (OS별 설정 차이 발생 가능)

**제안 docker-compose 구성**:
```yaml
services:
  mysql:
    image: mysql:8.0
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: atomiccv_dev

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
```

---

### ❓ [6] 브랜치 전략 & PR 머지 룰

| 항목 | 내용 |
|------|------|
| **목적** | 협업 충돌 방지, 배포 안정성 |
| **결정 기한** | Step 1 시작 전 |

**제안 브랜치 전략**:
```
main          ← 프로덕션 배포 브랜치 (직접 push 금지)
  └─ develop  ← 통합 개발 브랜치
       └─ feature/{기능명}  ← 기능 단위 개발
       └─ fix/{버그명}      ← 버그 수정
       └─ chore/{작업명}    ← 설정·의존성 변경
```

**PR 룰 (초안)**:
- PR 머지 방식: Squash and Merge (커밋 히스토리 정리)
- 승인자 수: 최소 1명 (팀 규모 고려)
- PR 템플릿: `docs/pipeline/pr-template.md` 사용

---

### ❓ [7] CLAUDE.md 세부 규칙 범위

| 항목 | 내용 |
|------|------|
| **목적** | Claude Code AI 에이전트가 지켜야 할 규칙 명세 |
| **결정 기한** | Step 1 시작 전 |

**범위 선택지**:

| 범위 | 포함 내용 |
|------|----------|
| **최소** | 프로젝트 개요, 패키지 컨벤션, API 응답 포맷 |
| **권장** ✅ | 최소 + PR 체크리스트, 코드 스타일, 테스트 작성 규칙 |
| **최대** | 권장 + Jira 티켓 자동화, Skill 연동 방법, 배포 트리거 조건 |

---

## 3. Step 2~3 전 결정 필요 (구현 설계 블로커)

---

### ❓ [8] Bounded Context 간 데이터 공유 방식

| 항목 | 내용 |
|------|------|
| **현황** | `BACKEND_DESIGN.md` 3-4절에 패턴 A(Port Interface)·패턴 B(Spring Event) 초안 작성됨 |
| **결정 기한** | Step 2 시작 전 (5/8 이전) |

**적용 매핑 확정 필요**:

| 출발 | 도착 | 패턴 | 확정 여부 |
|------|------|------|---------|
| resume → block | 블록 데이터 조회 | 패턴 A (Port) | ❓ |
| resume → analytics | 발행 후 세션 생성 | 패턴 B (Event) | ❓ |
| resume → feedback | 발행 후 슬롯 초기화 | 패턴 B (Event) | ❓ |
| feedback → resume | 피드백 집계 조회 | 패턴 A (Port) | ❓ |

---

### ❓ [9] 블록 버전 관리 전략

| 항목 | 내용 |
|------|------|
| **결정 기한** | ERD 확정 시 |

| 전략 | 장점 | 단점 |
|------|------|------|
| **A) snapshot_data JSON** (초안) | 구현 단순, 복원 용이 | 블록 개별 변경 이력 추적 불가 |
| **B) block_versions 별도 테이블** | 블록 변경 이력 추적 가능 | 테이블 증가, 조회 복잡도 상승 |

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

### ❓ [11] Redis HA 구성 여부

| 항목 | 내용 |
|------|------|
| **배경** | JWT Blacklist Redis 장애 시 로그아웃 무효화 실패 가능 (ADR-01) |
| **결정 기한** | Step 1 인프라 셋팅 시 |

| 선택지 | 비용 | 복잡도 |
|--------|------|--------|
| **단일 Redis** (MVP 권장) | 낮음 | 낮음 |
| Redis Sentinel | 중간 | 중간 |
| Redis Cluster | 높음 | 높음 |

MVP에서는 단일 Redis + 장애 발생 시 빠른 재시작 전략으로 운영 권장.

---

## 4. 코드 컨벤션

> 팀원 전원이 동의한 뒤 `CLAUDE.md`의 `rules/` 섹션에 공식 등록

---

### ❓ [12] 코드 컨벤션 확정

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
@Service
@Transactional
class PublishResumeUseCase(...) { ... }

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
| **방법** | PRD 기능 목록 기준 분담 협의 |
| **결정 기한** | 금일 회의 |

---

## 확정 사항 요약

| # | 항목 | 결정 내용 | 결정일 |
|---|------|----------|--------|
| 1 | 이메일 인증 | Gmail SMTP 사용 | 2026-04-30 |
| — | 인증 방식 | JWT + Redis Blacklist (ADR-01) | 2026-04-29 |
| — | 비동기 처리 | Kotlin Coroutine 우선 (ADR-02) | 2026-04-29 |
| — | PDF 생성 | FE 전담 (ADR-03) | 2026-04-29 |
| — | 아키텍처 | Full Monolith → Modular Monolith (ADR-04) | 2026-04-29 |

---

> 항목 확정 시 상태를 `❓ → ✅`로 변경하고 결정 내용 및 날짜를 기록해 주세요.