# Atomic CV — 작업 현황 트래킹

> 최종 업데이트: 2026-05-03 (1-3 인증 모듈 완료)
> 상태: 🔴 미시작 / 🟡 진행중 / 🟢 완료 / ⏸ 보류

---

## Phase 1: Foundation (환경 구축)

> 목표: 개발 환경, CI/CD, 인프라 기반 완성
> 기간: Step 1~2 (5/1 ~ 5/8)

### 1-1. CI/CD 인프라 구축

> 상세 플랜: `phases/phase-1-foundation/CICD_IMPL_PLAN.md`
> 트러블슈팅: `phases/phase-1-foundation/TROUBLESHOOTING.md`

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | EC2 서버 초기 환경 구성 (Docker, Nginx, AWS CLI) | | 🟢 |
| 2 | EC2 IAM Role 설정 (SSM 접근 권한) | | 🟢 |
| 3 | AWS SSM Parameter Store 환경변수 등록 | | 🟢 |
| 4 | SSL 인증서 설정 (Let's Encrypt + Certbot) | | ⏸ 도메인 확보 후 |
| 5 | Nginx Blue/Green 설정 | | 🟢 |
| 6 | Redis (ElastiCache TLS 연결) | | 🟢 |
| 7 | Dockerfile 작성 (eclipse-temurin:21-jre-alpine) | | 🟢 |
| 8 | 배포 스크립트 작성 (deploy.sh, rollback.sh) | | 🟢 |
| 9 | GitHub Actions Workflow (동적 보안그룹 방식) | | 🟢 |
| 10 | Spring Boot Actuator Health Check 설정 | | 🟢 |
| 11 | 첫 배포 검증 | | 🟢 |

### 1-2. 프로젝트 초기 설정

| # | 작업                                            | 담당 | 상태 |
|---|-----------------------------------------------|------|------|
| 1 | Spring Boot 프로젝트 생성 (Kotlin, Gradle)          | | 🟢 |
| 2 | 멀티모듈 구조 설정                                    | | 🟢 |
| 3 | ktlint + detekt 설정                            | | 🟢 |
| 4 | pre-commit hook 설정                            | | 🟢 |
| 5 | application.yml 프로파일 분리 (dev/prod)            | | 🟢 |
| 6 | CLAUDE.md 작성 (권장 범위 B)                        | | 🟢 |
| 7 | PR 템플릿 작성 (`.github/pull_request_template.md`) | | 🟢 |

### 1-3. 인증 (Auth) 구현

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | 소셜 로그인 구현 (Google / Kakao / Naver OAuth2) | | 🟢 |
| 2 | JWT Access Token 발급 / 검증 | | 🟢 |
| 3 | JWT Refresh Token 발급 / Redis 저장 | | 🟢 |
| 4 | Token Refresh API | | 🟢 |
| 5 | 로그아웃 (Blacklist 처리) | | 🟢 |

---

## Phase 2: Core (핵심 기능)

> 목표: 이력서 CRUD, 블록, 발행, 피드백 핵심 기능 완성
> 기간: Step 2~3 (5/8 ~)

### 2-1. 이력서 (Resume) 기능

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | 이력서 생성 / 조회 / 수정 / 삭제 | | 🔴 |
| 2 | 이력서 목록 조회 (페이지네이션) | | 🔴 |
| 3 | 이력서 발행 (슬러그 생성) | | 🔴 |
| 4 | 이력서 버전 스냅샷 저장 | | 🔴 |
| 5 | 공개 이력서 조회 (슬러그 기반) | | 🔴 |

### 2-2. 블록 (Block) 기능

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | 블록 생성 / 수정 / 삭제 | | 🔴 |
| 2 | 블록 순서 변경 | | 🔴 |
| 3 | block_versions 이력 저장 | | 🔴 |
| 4 | 블록 버전 복원 | | 🔴 |

### 2-3. 피드백 (Feedback) 기능

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | 피드백 슬롯 생성 (발행 이벤트 기반) | | 🔴 |
| 2 | 피드백 요청 생성 / 조회 | | 🔴 |
| 3 | 피드백 등록 (reviewer) | | 🔴 |
| 4 | 피드백 집계 조회 | | 🔴 |
| 5 | Rate Limit 처리 (Redis) | | 🔴 |

### 2-4. Analytics

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | 발행 후 조회 세션 생성 (Event 기반) | | 🔴 |
| 2 | 조회수 / 방문자 집계 API | | 🔴 |

---

## Phase 3: Advanced (고도화)

> 목표: AI 분석, 모니터링, 성능 최적화
> 기간: Phase 2 완료 후

### 3-1. AI 연동

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | Claude AI 이력서 분석 기능 | | 🔴 |
| 2 | AI 피드백 자동 생성 | | 🔴 |

### 3-2. 모니터링

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | 모니터링 스택 도입 (ELK or Prometheus+Grafana) | | 🔴 |
| 2 | 알람 설정 | | 🔴 |

### 3-3. 성능 / 안정성

| # | 작업 | 담당 | 상태 |
|---|------|------|------|
| 1 | WebFlux 전환 검토 | | 🔴 |
| 2 | Redis HA 구성 검토 | | 🔴 |
| 3 | MSA 전환 기준 검토 | | 🔴 |

---

## 미결 결정 사항 (회의 필요)

| # | 항목 | 관련 Phase | 상태 |
|---|------|-----------|------|
| 1 | 블록 저장 단위 (일괄 vs 개별) | Phase 2 | ⏸ 팀 논의 필요 |
| 2 | API 응답 포맷 (Bare vs 래퍼) | Phase 2 | ⏸ 프론트팀 협의 필요 |
| 3 | 웹 이력서 슬러그 구조 | Phase 2 | ⏸ 팀 회의 필요 |
| 4 | 작업 분배 확정 | 전체 | ⏸ PRD 기능 명세 완료 후 |

---

## 완료된 결정 사항

| 항목 | 결정 내용 | 결정일 |
|------|----------|--------|
| 이메일 인증 | 미사용 (소셜 로그인 전용으로 결정) | 2026-05-02 |
| Spring Boot 버전 | 3.x 유지 | 2026-04-30 |
| 로컬 개발 환경 | DB: AWS RDB / Redis: 로컬 | 2026-04-30 |
| 브랜치 전략 | main + feature/fix/chore, PR Merge, AI 승인 | 2026-04-30 |
| CLAUDE.md 범위 | 권장(B) + 실시간 업데이트 | 2026-04-30 |
| Bounded Context | Port(조회) / Event(발행 후 처리) | 2026-04-30 |
| 블록 버전 관리 | block_versions 테이블 + 스냅샷 JSON 병행 | 2026-04-30 |
| Redis HA | 단일 Redis (JWT Refresh Token 전용) | 2026-04-30 |
| 코드 컨벤션 | 초안 전체 확정 | 2026-04-30 |
| 인프라 | EC2(Ubuntu 26.04) + Nginx + SSM Parameter Store | 2026-05-01 |
| CI/CD | GitHub Actions + Blue/Green 배포 + 동적 보안그룹 | 2026-05-01 |
| Redis | ElastiCache TLS 연결 (ssl.enabled=true, Primary 엔드포인트) | 2026-05-01 |
| RDB | AWS RDS MySQL 8.4 + dev 데이터베이스 생성 | 2026-05-01 |
