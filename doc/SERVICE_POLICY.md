# Atomic CV — 서비스 정책서

> 버전: v1.0
> 작성일: 2026-05-07
> 상태: 확정
> 목적: 비즈니스 규칙 및 운영 정책 단일 참조 문서

---

## 목차

1. [인증·계정 정책](#1-인증계정-정책)
2. [이력서 정책](#2-이력서-정책)
3. [블록 정책](#3-블록-정책)
4. [피드백 정책](#4-피드백-정책)
5. [파일 업로드 정책](#5-파일-업로드-정책)
6. [데이터 보존·삭제 정책](#6-데이터-보존삭제-정책)
7. [MVP 보류 기능](#7-mvp-보류-기능)

---

## 1. 인증·계정 정책

### 로그인 방식

| 항목 | 정책 |
|------|------|
| 지원 방식 | 소셜 로그인 전용 (Google / Kakao / Naver OAuth2) |
| 일반 로그인 | **미지원** — 이메일+비밀번호 방식 사용 안 함 |
| 이메일 인증 | **미사용** — 소셜 이메일을 신뢰 (2026-05-02 결정) |

### 토큰 정책

| 항목 | 정책 |
|------|------|
| Access Token | JWT, 단기 만료, HttpOnly Cookie 전달 (XSS 방어) |
| Refresh Token | Redis 저장, 장기 만료 |
| 로그아웃 | Redis Blacklist 등록으로 즉시 무효화 |

### 소셜 계정 연동

- 한 유저가 여러 소셜 계정 동시 연동 가능 (Google + Kakao + Naver)
- 동일 이메일 + 다른 소셜 제공자 → 같은 `users` 레코드에 `social_accounts` 행 추가
- 다른 이메일 + 다른 소셜 제공자 → 별도 유저로 처리

---

## 2. 이력서 정책

### 슬러그 정책

| 항목 | 정책 |
|------|------|
| 생성 방식 | 랜덤 UUID 자동 생성 |
| 사용자 지정 | **미지원** (MVP 이후 검토) |
| 공개 URL 형식 | `/r/{slug}` |
| 비공개 처리 시 | 404 응답 (슬러그 존재 여부 노출 안 함) |

### 공개·비공개 정책

- `resumes.is_public = true` 이면 비인증 사용자도 슬러그로 조회 가능
- `is_public = false` 이면 소유자 본인만 접근 가능 (슬러그 직접 접근 시 404)

### 버전 관리

- MVP에서 버전 스냅샷(`resume_versions`) 미구현 — Phase 2 이후 추가
- PDF는 `resumes.pdf_s3_key`에 직접 저장 (이력서 1개당 PDF 1개)

---

## 3. 블록 정책

### 저장 방식

| 항목 | 정책 |
|------|------|
| 저장 단위 | **개별 저장** — 블록 타입별로 단건 생성·수정·삭제 API 제공 |

### 블록 순서

- 블록 라이브러리 전역 순서(`blocks.order_index`) **MVP 제외**
- 이력서 내 블록 순서는 `resume_blocks.order_index`로 관리

### Soft Delete

- 블록 삭제 시 `blocks.deleted_at` 설정 (실제 삭제 안 함)
- 이력서에서 블록 숨기기: `resume_blocks.is_visible = false` (삭제 없이 숨김)

---

## 4. 피드백 정책

### 익명화 정책

- 피드백 제출자 이름·이메일 **수집 안 함** (완전 익명)
- Rate Limit 및 어뷰징 방지 목적으로 `reviewer_ip`만 저장

### Rate Limit

| 항목 | 기준 |
|------|------|
| 제한 단위 | 동일 IP + 동일 이력서 |
| 제한 횟수 | 1시간 내 3회 |
| 초과 시 | 429 `RATE_LIMIT_EXCEEDED` 응답 |

### 피드백 접근

- 피드백 제출 API: 비인증 접근 가능 (채용담당자 대상)
- 피드백 조회 API: 이력서 소유자만 접근 가능

---

## 5. 파일 업로드 정책

| 파일 종류 | 허용 형식 | 최대 용량 |
|----------|----------|----------|
| 프로필 이미지 | jpg·png·webp | 5MB |
| 이력서 내 이미지 | jpg·png·webp | 5MB |
| PDF | pdf | 20MB |

- ATS 준수: PDF는 텍스트 레이어 필수, 이미지 PDF 불가 (FE 구현 시 준수)
- 폰트 임베딩 필수 (FE 구현 시 준수)

---

## 6. 데이터 보존·삭제 정책

### Soft Delete 적용 대상

| 테이블 | 처리 방식 | 비고 |
|--------|----------|------|
| `users` | Soft Delete (`deleted_at`) | 탈퇴 후 30일 보존 후 완전 삭제 예정 |
| `blocks` | Soft Delete (`deleted_at`) | 이력서 참조 유지 |
| `resumes` | Soft Delete (`deleted_at`) | 공개 URL 즉시 404 처리 |

### Hard Delete 대상

| 테이블 | 처리 방식 |
|--------|----------|
| `feedbacks`, `feedback_tags` | Hard Delete |
| `view_sessions`, `section_dwells` | Hard Delete |
| `social_accounts` | Hard Delete (탈퇴 시) |

### 회원탈퇴 처리

1. `users.is_active = false`, `deleted_at` 설정
2. 탈퇴 후 즉시 로그인 불가
3. 30일 경과 후 완전 삭제 예정 (스케줄러 구현 — Phase 2 이후)

---

## 7. MVP 보류 기능

아래 기능은 설계는 완료됐으나 MVP 구현 범위에서 제외됨.

| 기능 | 이유 | 예정 Phase |
|------|------|-----------|
| `resume_versions` (버전 스냅샷) | MVP 복잡도 감소 | Phase 2 |
| `notifications` (알림) | MVP 범위 축소 | Phase 2 |
| `block_versions` (블록 이력) | MVP 복잡도 감소 | Phase 2 |
| `blocks.order_index` (라이브러리 전역 순서) | 이력서 내 순서로 충분 | Phase 2 |
| 버전 롤백 API | resume_versions 보류에 따른 연동 보류 | Phase 2 |
| 탈퇴 후 완전 삭제 스케줄러 | MVP 운영 우선 | Phase 2 |
| `resumes.type` ENUM 값 확정 | ENUM('PDF', 'WEB') 확정 (2026-05-07) | — |
