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
| `doc/SERVICE_POLICY.md` | 서비스 정책 — 비즈니스 규칙 단일 참조 |
| `doc/discussion.md` | 팀 확정 사항 (코드 컨벤션, 아키텍처 결정) |
| `doc/INFRA_DESIGN.md` | 인프라 아키텍처, 환경변수 관리 |
| `doc/API_RESPONSE.md` | API 응답 포맷, 에러코드 명세 |
| `doc/CONVENTION.md` | Claude 작업 공용 프롬프트 패턴 |
| `doc/MODULE_STRUCTURE.md` | 멀티모듈 구조 설계 (모듈 목록, 패키지 구조, 의존성) |
| `doc/DETEKT_RULES.md` | Detekt / ktlint 위반 패턴 및 로컬 검사 명령어 |
| `doc/phases/` | Phase별 세부 구현 계획 및 트러블슈팅 |
| `module-{name}/CLAUDE.md` | 모듈 전용 설계 명세 (해당 모듈 작업 시 참조) |

---

## 환경변수 위치

```
GitHub Secrets          → CI/CD 전용 (AWS 접근키, SSH 키)
AWS SSM Parameter Store → 앱 실행 환경변수 (경로: /atomiccv/prod/*)
  - DB_URL, DB_USERNAME, DB_PASSWORD
  - JWT_SECRET, JWT_EXPIRY
  - REDIS_HOST
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
7. **모듈 전용 설계 문서는 해당 모듈 내부에 작성한다** — `module-{name}/CLAUDE.md`에 저장하며 `doc/`에 중복 작성하지 않는다. 모듈 작업 시작 전 해당 파일을 먼저 읽는다.
8. **TASK 완료 시 GitHub Wiki에 기술 결정 기록을 작성하고 push한다** — 파일명: `Task-{Phase번호}-{서브번호}-{기능명}.md`. 기술 의사결정·Kotlin 패턴·구현 내용·트레이드오프를 포함한다. 상세 프로세스 및 템플릿: `doc/CONVENTION.md` [8. Wiki 문서화 프로세스] 참조.

---

## 행동 원칙

- 불확실하면 가정하지 말고 질문한다. 여러 해석이 가능하면 선택지를 제시하고 임의로 선택하지 않는다.
- 요청받은 것만 구현한다. 추측성 기능 추가·단일 사용 코드 추상화 금지. 200줄이 50줄로 가능하면 다시 작성한다.
- 수정 요청을 받은 코드만 변경한다. 인접 코드를 "개선"하지 않으며, 데드코드 발견 시 삭제 말고 언급만 한다.
- 다단계 작업은 시작 전 계획을 간략히 제시하고, 완료 기준을 명확히 정의한 후 검증해 완료를 선언한다.

---

## 아키텍처 규칙

### 의존성 방향

```
interfaces → application → domain
infrastructure → domain
```

- `application`은 `domain`만 참조 (JPA, Spring 등 import 금지)
- `interfaces`는 `application`만 참조 (`domain` 직접 참조 금지)
- 트랜잭션 경계는 `application` 레이어 UseCase에만 선언

### 클래스 Suffix

| 대상 | Suffix | 예시 |
|------|--------|------|
| Use Case | `UseCase` | `PublishResumeUseCase` |
| Application Command | `Command` | `PublishResumeCommand` |
| Application Query | `Query` | `GetResumeQuery` |
| Repository Interface | `Repository` | `ResumeRepository` |
| Repository Impl | `RepositoryImpl` | `ResumeRepositoryImpl` |
| Port Interface | `Port` | `BlockQueryPort` |
| Port Adapter | `Adapter` | `BlockQueryAdapter` |
| Event | `Event` | `ResumePublishedEvent` |
| Event Handler | `EventHandler` | `ResumePublishedEventHandler` |
| REST Controller | `Controller` | `ResumeController` |
| Request/Response DTO | `Request` / `Response` | `PublishResumeRequest` |
| External Client | `Client` | `GoogleOAuthClient` |
| Config 클래스 | `Config` | `SecurityConfig` |
| Exception | `Exception` | `ResumeNotFoundException` |

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
  └─ dev ← 통합 브랜치 (직접 push 금지, main으로 PR 머지)
       └─ feature/{기능명}
       └─ fix/{버그명}
       └─ chore/{작업명}
```

작업 흐름: feature/fix/chore 브랜치 생성 → dev로 PR → 머지 후 dev → main PR

PR 머지 방식: Merge | 승인: AI | 템플릿: `.github/pull_request_template.md`

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

## Detekt / ktlint

코드 작성 전 위반 패턴 확인 필수 — CI 실패의 주원인.
> 상세 패턴 및 로컬 검사 명령어: `doc/DETEKT_RULES.md`

---

## 슬래시 커맨드

| 커맨드 | 설명 |
|--------|------|
| `/kship [PR 제목]` | 테스트 실행 → 커밋 → 푸시 → PR 생성 자동화 |
| `/meeting` | 백엔드 팀 회의 진행 (discussion.md 기반) |

### /kship 동작 순서

1. `git status` / `git diff` / `git log` 병렬 실행
2. 민감 파일 제외 후 선택적 스테이징 (`.env`, `build/`, `*.pem` 등 제외)
3. `./gradlew test --continue` 실행 → XML 리포트 파싱
   - 빌드 오류 시 즉시 중단
   - 테스트 실패 시 목록 보여주고 사용자 확인
4. HEREDOC 방식으로 커밋
5. `git push -u origin HEAD`
6. `gh pr create --base dev` — 테스트 결과 기반 Test Plan 자동 생성
