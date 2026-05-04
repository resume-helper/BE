# Worklog Dashboard — Design Spec

**날짜:** 2026-05-04  
**작성자:** Claude (brainstorming 세션)  
**상태:** 합의 완료, 구현 대기

---

## 개요

팀원 전체가 오늘의 미팅, GitHub 활동, 코드 활동을 한 눈에 확인할 수 있는 팀 공유 Worklog 대시보드.  
기존 Atomic CV 백엔드에 `module-worklog` 모듈로 추가한다.

---

## 기술 스택

| 항목 | 선택 | 이유 |
|------|------|------|
| 백엔드 | Spring Boot (기존 모듈 추가) | 추가 인프라 없음 |
| 프론트엔드 | Thymeleaf + HTMX | npm 빌드 파이프라인 불필요 |
| 서버 | t3.micro (기존 EC2) | 추가 비용 없음 |
| 음성→텍스트 | OpenAI Whisper API | t3.micro에서 로컬 모델 실행 불가 |
| 텍스트 요약 | OpenAI GPT | Whisper와 동일 API |
| GitHub 데이터 | GitHub REST API | 커밋/PR/이슈 조회 |
| 캐시 | Caffeine (메모리) | Rate Limit 대응, TTL 10분 |
| 실시간성 | 페이지 로드 시 최신 데이터 | WebSocket 불필요 |

---

## 기능 범위

### 포함
- **오늘의 미팅:** 음성 파일 업로드 → Whisper API 변환 → GPT 요약 → HTMX partial 렌더링
- **GitHub 활동:** 오늘 날짜 기준 커밋, PR, 이슈 (조직: `resume-helper`)
- **코드 활동:** 오늘 커밋 수, 변경 파일 수, 추가/삭제 라인 수

### 제외
- AI 세션 추적
- 실시간 폴링 / WebSocket
- 팀원별 개인 통계

---

## 화면 구성

**메인 대시보드 (`/worklog`)**

```
┌─────────────────────────────────────────────────────┐
│  Worklog Dashboard          📅 2026-05-04  [< >]    │
├─────────────────┬───────────────────────────────────┤
│  오늘의 미팅    │  GitHub 활동                       │
│                 │                                   │
│  [+ 음성 업로드]│  커밋 (5)  PR (2)  이슈 (1)       │
│                 │                                   │
│  • 스프린트 회의│  ● feat(auth): JWT 갱신 로직 추가  │
│    10:00 - 요약 │  ● fix(resume): 저장 버그 수정     │
│    보기 ▼      │  ● PR #3 merged: auth 모듈         │
│                 │                                   │
│  • 아키텍처 리뷰│                                   │
│    14:00 - 요약 │                                   │
│    보기 ▼      │                                   │
├─────────────────┴───────────────────────────────────┤
│  코드 활동 요약                                      │
│  오늘 커밋 5개 | 변경 파일 12개 | +234 -89 lines    │
└─────────────────────────────────────────────────────┘
```

---

## 모듈 구조

```
module-worklog/
├── domain/
│   └── model/
│       ├── Meeting.kt
│       └── WorklogDate.kt
├── application/
│   └── usecase/
│       ├── UploadMeetingUseCase.kt
│       ├── GetDailyWorklogUseCase.kt
│       └── GetGithubActivityUseCase.kt
├── infrastructure/
│   ├── persistence/
│   ├── external/
│   │   ├── WhisperClient.kt
│   │   ├── GptSummaryClient.kt
│   │   └── GithubClient.kt
│   └── cache/
│       └── GithubActivityCache.kt
└── interfaces/
    └── web/
        ├── WorklogController.kt
        ├── MeetingApiController.kt
        └── templates/worklog/
            ├── dashboard.html
            └── meeting-summary.html
```

---

## 도메인 모델

```kotlin
Meeting(
    id: Long,
    title: String,           // 파일명 또는 사용자 입력
    meetingDate: LocalDate,
    audioFilePath: String,   // 임시 저장 경로 (변환 후 삭제)
    transcript: String,      // Whisper 변환 텍스트
    summary: String,         // GPT 요약
    createdAt: LocalDateTime
)
```

---

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/worklog` | 대시보드 페이지 (오늘 날짜 기본) |
| `GET` | `/worklog?date=YYYY-MM-DD` | 날짜 선택 |
| `POST` | `/worklog/meetings` | 음성 파일 업로드 (multipart) |
| `GET` | `/worklog/meetings/{id}/summary` | HTMX partial 요약 카드 |

---

## 외부 API 연동

### OpenAI Whisper + GPT
- 음성 처리: `@Async` 비동기 처리 (업로드 후 결과 대기)
- 파일 크기 제한: 25MB (Whisper API 제한)
- 실패 시: `summary = "요약 실패 — 다시 시도해주세요"` 저장, Meeting 레코드는 보존

### GitHub API
- 조직: `resume-helper`
- 캐시: Caffeine TTL 10분 (Rate Limit 초과 방지)
- 실패 시: "GitHub 데이터를 불러올 수 없습니다" 메시지 표시

---

## 보안

- `/worklog` 경로: Spring Security 기존 인증 필터 적용 (로그인 팀원만 접근)
- 음성 파일 타입 검증: `audio/mpeg`, `audio/mp4`, `audio/wav` 만 허용
- 음성 파일: Whisper 변환 완료 후 서버에서 즉시 삭제 (`/var/worklog/audio/` 임시 저장)

---

## 환경변수 (AWS SSM)

```
/atomiccv/prod/OPENAI_API_KEY
/atomiccv/prod/GITHUB_TOKEN
```
