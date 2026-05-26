# Block Draft (임시저장) 도메인 설계

> 작성일: 2026-05-22
> 상태: 확정
> 관련 정책: SERVICE_POLICY.md §8.2.3–8.2.6

---

## 1. 개요

블록 작성 중 임시저장 기능을 제공한다. 사용자는 필수값을 모두 입력하지 않아도 중간 상태를 저장할 수 있으며, 나중에 불러와 이어 작성할 수 있다.

### 핵심 정책 요약

| 정책 | 내용 |
|------|------|
| 대표 제목 | `Block.title`과 동일 필드. 비어 있으면 임시저장 불가 |
| contentJson | 부분 저장 허용 (빈 `{}` 포함) |
| 만료 기한 | 임시저장 시점 + 14일 (`expires_at`) |
| 덮어쓰기 | 불러온 드래프트 수정 후 재저장 시 해당 항목 덮어씌움 |
| 최종저장 연동 | 블록 최종저장 완료 후 FE가 해당 드래프트 직접 삭제 |
| "작성중" 표시 | DB 컬럼 아님 — 목록 조회 시 `currentDraftId` 파라미터로 계산 |

---

## 2. 모듈 배치

`module-resume` 내에 추가한다. 별도 모듈 분리 없음.

```
module-resume/
  domain/model/BlockDraft.kt
  domain/repository/BlockDraftRepository.kt
  application/usecase/
    CreateBlockDraftUseCase.kt
    UpdateBlockDraftUseCase.kt
    GetBlockDraftsUseCase.kt
    GetBlockDraftUseCase.kt
    DeleteBlockDraftUseCase.kt
    DeleteBlockDraftsUseCase.kt
    DeleteAllBlockDraftsByTypeUseCase.kt
  infrastructure/persistence/
    BlockDraftJpaEntity.kt
    BlockDraftJpaRepository.kt
    BlockDraftRepositoryImpl.kt
  infrastructure/scheduler/
    BlockDraftExpireScheduler.kt
  interfaces/rest/
    BlockDraftController.kt
```

---

## 3. 도메인 모델

```kotlin
data class BlockDraft(
    val id: Long = 0,
    val userId: Long,
    val blockType: BlockType,
    val title: String,        // 대표 제목 (Block.title과 동일), 비어있으면 저장 불가
    val contentJson: String,  // 부분 저장 허용. 기본값 "{}"
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
    fun isOwnedBy(ownerId: Long): Boolean = userId == ownerId
}
```

---

## 4. DB 스키마

### `block_drafts` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유 식별자 |
| user_id | BIGINT | NOT NULL | 소유자 |
| block_type | ENUM(BlockType) | NOT NULL | 블록 타입 |
| title | VARCHAR(200) | NOT NULL | 대표 제목 (최소 1자) |
| content_json | JSON | NULL 허용 | 부분 저장 본문 |
| expires_at | DATETIME | NOT NULL | 만료 일시 (저장 시점 + 14일) |
| created_at | DATETIME | NOT NULL | 생성 일시 |
| updated_at | DATETIME | NOT NULL | 최종 수정 일시 |

**인덱스**

| 인덱스명 | 컬럼 | 목적 |
|---------|------|------|
| `idx_block_drafts_user_type` | (user_id, block_type) | 타입별 목록 조회 |
| `idx_block_drafts_expires_at` | expires_at | 스케줄러 만료 삭제 |

---

## 5. Repository 인터페이스

```kotlin
interface BlockDraftRepository {
    fun save(draft: BlockDraft): BlockDraft
    fun findById(id: Long): BlockDraft?
    fun findAllByUserIdAndBlockType(userId: Long, blockType: BlockType): List<BlockDraft>
    fun deleteById(id: Long)
    fun deleteAllByIds(ids: List<Long>)
    fun deleteAllByUserIdAndBlockType(userId: Long, blockType: BlockType)
    fun deleteAllExpiredBefore(threshold: LocalDateTime): Int
}
```

---

## 6. Use Cases

### 6.1 `CreateBlockDraftUseCase`

새 임시저장 생성.

- **Command**: `CreateBlockDraftCommand(userId, blockType, title, contentJson)`
- **검증**: `title.isBlank()` → `BlockDraftTitleRequiredException`
- **처리**: `expiresAt = now + 14일` 설정 후 저장
- **반환**: `BlockDraft`

### 6.2 `UpdateBlockDraftUseCase`

기존 드래프트 덮어쓰기 (불러온 드래프트 수정 후 재저장).

- **Command**: `UpdateBlockDraftCommand(draftId, userId, title, contentJson)`
- **검증**: 존재 여부 확인 → `BlockDraftNotFoundException`, 소유자 확인 → `BlockDraftForbiddenException`, `title.isBlank()` → `BlockDraftTitleRequiredException`
- **처리**: title·contentJson 갱신, `expiresAt = now + 14일` 갱신 (만료 연장)
- **반환**: `BlockDraft`

### 6.3 `GetBlockDraftsUseCase`

블록 타입별 목록 조회. "작성중" 배지를 위한 `currentDraftId` 포함.

- **Query**: `GetBlockDraftsQuery(userId, blockType, currentDraftId: Long?)`
- **처리**: 만료되지 않은 항목 필터링 후 반환. `currentDraftId`와 일치하는 항목에 `isActive = true`
- **반환**: `List<BlockDraftSummary>` (id, title, updatedAt, isActive)
- **정렬**: `updatedAt` 내림차순

### 6.4 `GetBlockDraftUseCase`

단건 조회 (드래프트 불러오기).

- **Query**: `GetBlockDraftQuery(draftId, userId)`
- **검증**: 존재 여부, 소유자, 만료 여부(`isExpired()`) 확인
- **반환**: `BlockDraft`

### 6.5 `DeleteBlockDraftUseCase`

단건 삭제.

- **Command**: `DeleteBlockDraftCommand(draftId, userId)`
- **검증**: 존재 여부, 소유자 확인
- **처리**: Hard Delete

### 6.6 `DeleteBlockDraftsUseCase`

선택 삭제 (ID 리스트).

- **Command**: `DeleteBlockDraftsCommand(draftIds: List<Long>, userId)`
- **검증**: 모든 ID에 대해 소유자 확인. 소유자가 아닌 항목이 하나라도 있으면 `BlockDraftForbiddenException`
- **처리**: Hard Delete (일괄)

### 6.7 `DeleteAllBlockDraftsByTypeUseCase`

타입별 전체 삭제.

- **Command**: `DeleteAllBlockDraftsByTypeCommand(userId, blockType)`
- **처리**: `user_id + block_type` 조건으로 전체 Hard Delete

---

## 7. REST API

`BlockDraftController` — `GET/api/block-drafts` 외 모두 인증 필수.

### 엔드포인트 정의

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/block-drafts` | 새 임시저장 생성 |
| GET | `/api/block-drafts?type={BlockType}&currentDraftId={id}` | 블록 타입별 목록 조회 |
| GET | `/api/block-drafts/{id}` | 단건 불러오기 |
| PUT | `/api/block-drafts/{id}` | 덮어쓰기 |
| DELETE | `/api/block-drafts/{id}` | 단건 삭제 |
| DELETE | `/api/block-drafts/batch` (body: `{ids: [1,2,3]}`) | 선택 삭제 |
| DELETE | `/api/block-drafts?type={BlockType}` | 타입별 전체 삭제 |

### Request / Response 명세

**POST `/api/block-drafts`**
```json
// Request
{
  "blockType": "CAREER",
  "title": "카카오 백엔드 개발자",
  "contentJson": {"company": "카카오", "startDate": "2024-01"}
}

// Response 200
{
  "success": true,
  "data": {
    "id": 5,
    "blockType": "CAREER",
    "title": "카카오 백엔드 개발자",
    "contentJson": {"company": "카카오", "startDate": "2024-01"},
    "expiresAt": "2026-06-05T14:30:00",
    "createdAt": "2026-05-22T14:30:00",
    "updatedAt": "2026-05-22T14:30:00"
  }
}
```

**GET `/api/block-drafts?type=CAREER&currentDraftId=5`**
```json
// Response 200
{
  "success": true,
  "data": [
    {
      "id": 5,
      "title": "카카오 백엔드 개발자",
      "updatedAt": "2026-05-22T14:30:00",
      "isActive": true
    },
    {
      "id": 3,
      "title": "네이버 서버 개발자",
      "updatedAt": "2026-05-20T09:10:00",
      "isActive": false
    }
  ]
}
```

---

## 8. 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| `BLOCK_DRAFT_NOT_FOUND` | 404 | 존재하지 않거나 만료된 드래프트 |
| `BLOCK_DRAFT_FORBIDDEN` | 403 | 본인 소유가 아닌 드래프트 접근 |
| `BLOCK_DRAFT_TITLE_REQUIRED` | 400 | 대표 제목(title)이 비어 있을 때 |

---

## 9. 만료 스케줄러

`BlockDraftExpireScheduler` — infrastructure 레이어.

```kotlin
@Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
fun deleteExpiredDrafts() {
    val count = blockDraftRepository.deleteAllExpiredBefore(LocalDateTime.now())
    // 로그 기록
}
```

---

## 10. 최종저장 연동 흐름

임시저장 → 블록 최종저장 시 드래프트 삭제는 **FE가 직접 DELETE 호출**하는 방식으로 처리한다.

```
FE: POST /api/blocks       → 블록 최종저장
FE: DELETE /api/block-drafts/{id}  → 드래프트 삭제
```

BE에서 블록 저장과 드래프트 삭제를 같은 트랜잭션으로 묶지 않는 이유: 임시저장 없이 바로 블록을 생성하는 케이스도 존재하기 때문.

---

## 11. 구현 제외 범위 (MVP)

- 드래프트 개수 상한 없음 (만료 14일로만 관리)
- 드래프트 내 첨부파일 지원 없음
