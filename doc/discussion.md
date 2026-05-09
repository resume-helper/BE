# Atomic CV — 팀 확정 사항

> 상세 논의 이력: `doc/archive/discussion_history.md`
> 상태 표기: ✅ 확정 / ❓ 논의 필요 / 🔜 나중에 결정

---

## 확정 사항

| # | 항목 | 결정 내용 | 결정일 |
|---|------|----------|--------|
| 1 | 이메일 인증 | 미사용 (소셜 로그인 전용) | 2026-05-02 |
| 2 | ERD 설계 | DDL 확정 (`doc/erd_cloud_import.sql`), 세부 결정: 이력[2] 참조 | 2026-05-07 |
| 3 | API 응답 포맷 | 공통 래퍼 `ApiResponse<T>` 사용, 상세: `doc/API_RESPONSE.md` | 2026-05-01 |
| 4 | Spring Boot 버전 | 3.x 유지 (Kotlin Boot 4.x 미호환) | 2026-04-30 |
| 5 | 로컬 개발 환경 | DB: AWS RDB 연결 / Redis: 로컬 구동 | 2026-04-30 |
| 6 | 브랜치 전략 | main + feature/fix/chore, PR: Merge / AI 승인 | 2026-04-30 |
| 7 | CLAUDE.md 범위 | 권장(B) 범위로 시작, 실시간 업데이트 | 2026-04-30 |
| 8 | Bounded Context | Port(조회) / Event(발행 후 처리) — 상세: 이력[8] 참조 | 2026-04-30 |
| 9 | 블록 버전 관리 | block_versions 미구현, 버전 스냅샷 전체 보류 (Phase 2) | 2026-05-07 |
| 10 | 슬러그 구조 | 랜덤 UUID 자동 생성, 사용자 지정 미지원 (MVP 이후 검토) | 2026-05-07 |
| 11 | Redis HA | 단일 Redis (JWT Refresh Token 관리 전용) | 2026-04-30 |
| 12 | 코드 컨벤션 | 네이밍/Suffix/패키지/의존성/예외/트랜잭션/테스트/도구/주석 전체 확정 — 상세: 이력[12] 참조 | 2026-04-30 |
| — | 인증 방식 | JWT + Redis Blacklist + HttpOnly Cookie (ADR-01) | 2026-04-29 |
| — | 비동기 처리 | Kotlin Coroutine 우선 (ADR-02) | 2026-04-29 |
| — | PDF 생성 | FE 전담 (ADR-03) | 2026-04-29 |
| — | 아키텍처 | Full Monolith → Modular Monolith (ADR-04) | 2026-04-29 |
| — | 서비스 정책 | `doc/SERVICE_POLICY.md` 작성 완료 (비즈니스 규칙 단일 참조) | 2026-05-07 |

---

## 미결 사항

### 🔜 [13] 모니터링 스택 선택

선택지: ELK Stack vs Prometheus + Grafana — Phase 2 이후 결정.

---

### 🔜 [14] WebFlux 전환 시점 기준

현황: MVC + Coroutine 사용. 트리거: AI 연동(Phase 2) 또는 동시 접속자 급증 시.

---

### 🔜 [15] MSA 전환 트리거 기준

현황: Modular Monolith 유지. 기준 초안: 일 트래픽 10만 요청 초과 또는 AI·결제 모듈 팀 분리 시.

---

### ❓ [16] 작업 분배 확정

PRD 기능 명세 완료 후 협의 예정.

---

### ❓ [17] module-feedback / module-analytics 분리 여부

- A안: 분리 유지 (`module-feedback` + `module-analytics`)
- B안: `module-engagement`로 통합 (두 모듈 모두 `ResumePublishedEvent` 기반)
- C안: `module-resume`에 흡수

멀티모듈 구조 설정 착수 전 결정 필요 (`doc/TASKS.md` 1-2-2).
