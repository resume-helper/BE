# CLAUDE.md — Atomic CV 백엔드

> 이 파일은 Claude가 작업 시 항상 준수해야 할 규칙을 정의한다.
> 상세 컨벤션: `doc/discussion.md` | 작업 현황: `doc/TASKS.md`

---

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **서비스** | Atomic CV — AI 기반 이력서 관리·피드백 플랫폼 |
| **언어** | Kotlin |
| **프레임워크** | Spring Boot 3.x, Spring Security |
| **DB** | JPA + QueryDSL, AWS RDS (로컬: AWS RDB 직접 연결) |
| **캐시** | Redis (로컬: Docker 구동) |
| **인증** | JWT + Redis Blacklist |
| **아키텍처** | Modular Monolith (DDD, Hexagonal) |
| **패키지 루트** | `com.atomiccv` |

---

## 문서 인덱스

| 문서 | 용도 |
|------|------|
| `doc/TASKS.md` | 작업 현황 트래킹 (Phase / 상태) |
| `doc/prd.md` | 제품 요구사항 — 기능 구현 범위 판단 기준 |
| `doc/ERD_DRAFT.md` | DB 설계 — 엔티티 및 관계 참조 |
| `doc/discussion.md` | 팀 확정 사항 (코드 컨벤션, 아키텍처 결정) |
| `doc/INFRA_DESIGN.md` | 인프라 아키텍처, 환경변수 관리 |
| `doc/API_RESPONSE.md` | API 응답 포맷, 에러코드 명세 |
| `doc/CONVENTION.md` | Claude 작업 공용 프롬프트 패턴 |
| `doc/phases/` | Phase별 세부 구현 계획 및 트러블슈팅 |

---

## 환경변수 위치

```
GitHub Secrets          → CI/CD 전용 (AWS 접근키, SSH 키)
AWS SSM Parameter Store → 앱 실행 환경변수 (경로: /atomiccv/prod/*)
  - DB_URL, DB_USERNAME, DB_PASSWORD
  - JWT_SECRET, JWT_EXPIRY
  - REDIS_HOST
  - SMTP_USERNAME, SMTP_PASSWORD
```

**규칙:** 환경변수는 절대 하드코딩하지 않는다. `.env` 파일과 키 파일은 커밋하지 않는다.

---

## Claude 작업 필수 규칙

1. **파일 수정 전 반드시 해당 파일 전체를 먼저 읽는다.**
2. **모듈 간 직접 참조 코드를 생성하지 않는다** — Port Interface 또는 Event를 통해 통신한다.
3. **DDD 철학을 준수한다** — domain은 외부 라이브러리를 import하지 않는다.
4. **새 기능은 UseCase 단위로 분리한다** — 하나의 UseCase = 하나의 비즈니스 행위.
5. **응답은 한국어로, 코드 주석은 한국어 허용.**
6. **작업 완료 시 `doc/TASKS.md`의 해당 항목 상태를 🟢로 업데이트한다.**

---

## 행동 원칙

### 코딩 전 생각하기
- 불확실하면 가정하지 말고 질문한다.
- 여러 해석이 가능하면 선택지를 제시한다 — 임의로 선택하지 않는다.
- 더 단순한 방법이 있으면 먼저 제안한다.

### 단순함 우선
- 요청받은 것만 구현한다. 추측성 기능을 추가하지 않는다.
- 단일 사용 코드에 추상화를 만들지 않는다.
- 200줄이 50줄로 가능하면 다시 작성한다.

### 외과적 수정
- 수정 요청을 받은 코드만 변경한다. 인접 코드를 "개선"하지 않는다.
- 관련 없는 데드코드를 발견하면 삭제하지 말고 언급만 한다.
- 기존 코드 스타일을 그대로 따른다.

### 목표 기반 실행
- 다단계 작업은 시작 전에 계획을 간략히 제시한다.
- 완료 기준을 명확히 정의하고 검증 후 완료를 선언한다.

---

## 아키텍처 규칙

### 패키지 구조

```
com.atomiccv.{domain}.domain.model.*
com.atomiccv.{domain}.application.usecase.*
com.atomiccv.{domain}.infrastructure.persistence.*
com.atomiccv.{domain}.interfaces.rest.*
```

### 의존성 방향

```
interfaces → application → domain
infrastructure → domain
```

- `application`은 `domain`만 참조 (JPA, Spring 등 import 금지)
- `interfaces`는 `application`만 참조 (`domain` 직접 참조 금지)
- 트랜잭션 경계는 `application` 레이어 UseCase에만 선언

### 클래스 Suffix 핵심

| 대상 | Suffix | 예시 |
|------|--------|------|
| Use Case | `UseCase` | `PublishResumeUseCase` |
| Repository Interface | `Repository` | `ResumeRepository` |
| Repository Impl | `RepositoryImpl` | `ResumeRepositoryImpl` |
| REST Controller | `Controller` | `ResumeController` |
| Request/Response DTO | `Request` / `Response` | `PublishResumeRequest` |
| Port Interface | `Port` | `BlockQueryPort` |
| Event | `Event` | `ResumePublishedEvent` |

> 전체 컨벤션: `doc/discussion.md` [12]

---

## API 응답 규칙

- 성공: `ApiResponse<T>` 공통 래퍼 사용
- 에러: `BusinessException` 상속, `GlobalExceptionHandler`에서 처리
- 상세 포맷: `doc/API_RESPONSE.md`

---

## 브랜치 전략

```
main ← 프로덕션 (직접 push 금지)
  └─ feature/{기능명}
  └─ fix/{버그명}
  └─ chore/{작업명}
```

PR 머지 방식: Merge | 승인: AI | 템플릿: `docs/pipeline/pr-template.md`

---

## 테스트 네이밍

```kotlin
@Test
fun `이력서 발행 시 slug가 생성되고 버전 스냅샷이 저장된다`() { }

@Test
fun `존재하지 않는 이력서 발행 시 ResumeNotFoundException이 발생한다`() { }
```

---

## 커밋 메시지

```
feat(module): [기능명] 구현
fix(module): [버그명] 수정
refactor(module): [대상] 리팩터링
docs: [문서명] 업데이트
test(module): [테스트 대상] 추가
ci: CI/CD 설정 변경
chore: 의존성·설정 변경
```

---

## 슬래시 커맨드

| 커맨드 | 설명 |
|--------|------|
| `/ship [PR 제목]` | 테스트 실행 → 커밋 → 푸시 → PR 생성 자동화 |
| `/meeting` | 백엔드 팀 회의 진행 (discussion.md 기반) |

### /ship 동작 순서

1. `git status` / `git diff` / `git log` 병렬 실행
2. 민감 파일 제외 후 선택적 스테이징 (`.env`, `build/`, `*.pem` 등 제외)
3. `./gradlew test --continue` 실행 → XML 리포트 파싱
   - 빌드 오류 시 즉시 중단
   - 테스트 실패 시 목록 보여주고 사용자 확인
4. HEREDOC 방식으로 커밋
5. `git push -u origin HEAD`
6. `gh pr create --base main` — 테스트 결과 기반 Test Plan 자동 생성
