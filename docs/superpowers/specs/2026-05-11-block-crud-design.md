# Block CRUD 설계 스펙

> 작성일: 2026-05-11
> Phase: 2-1
> 브랜치: feature/block-crud

---

## 개요

블록(Block)은 이력서를 구성하는 콘텐츠 단위다. 사용자가 경력·기술·프로젝트 등을 블록으로 저장해두고, 이후 이력서를 구성할 때 재사용한다. 이번 구현 범위는 블록 라이브러리 CRUD(생성·조회·수정·삭제)이며, 이력서 내 블록 순서 변경(`resume_blocks`)은 2-2에서 처리한다.

---

## API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/api/blocks?type={type}` | 내 블록 목록 조회 (type 필터 optional) | 필요 |
| `POST` | `/api/blocks` | 블록 단건 생성 | 필요 |
| `PUT` | `/api/blocks/{id}` | 블록 단건 수정 | 필요 |
| `DELETE` | `/api/blocks/{id}` | 블록 삭제 (Soft Delete) | 필요 |

### 요청/응답 예시

**POST /api/blocks**
```json
{
  "type": "CAREER",
  "title": "카카오 백엔드 개발자",
  "contentJson": "{\"company\":\"카카오\",\"period\":\"2022.01~2024.03\"}"
}
```

**응답 (공통)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "type": "CAREER",
    "title": "카카오 백엔드 개발자",
    "contentJson": "{...}",
    "createdAt": "2026-05-11T10:00:00",
    "updatedAt": "2026-05-11T10:00:00"
  }
}
```

---

## 도메인 모델

### Block

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 소유자 |
| type | BlockType | CAREER / SKILL / PROJECT / EDUCATION / CERTIFICATE / ACTIVITY / CUSTOM |
| title | String | 블록 제목 (최대 200자) |
| contentJson | String | 타입별 JSON 본문 |
| createdAt | LocalDateTime | 생성 일시 |
| updatedAt | LocalDateTime | 수정 일시 |
| deletedAt | LocalDateTime? | Soft Delete 일시 |

---

## 비즈니스 규칙

- 조회·수정·삭제 시 `userId`가 토큰의 userId와 다르면 403
- 삭제된 블록(`deletedAt != null`) 접근 시 404
- `contentJson` 유효성 검사는 FE 책임 (BE는 String 저장)
- `resume_blocks` 연동 및 순서 변경은 2-2에서 구현

---

## 파일 구조

```
module-resume/src/main/kotlin/com/atomiccv/resume/
├── domain/
│   ├── model/
│   │   └── Block.kt
│   └── repository/
│       └── BlockRepository.kt
├── application/usecase/
│   ├── CreateBlockUseCase.kt
│   ├── UpdateBlockUseCase.kt
│   ├── DeleteBlockUseCase.kt
│   └── GetBlocksUseCase.kt
├── infrastructure/
│   ├── persistence/
│   │   ├── BlockJpaEntity.kt
│   │   ├── BlockJpaRepository.kt
│   │   └── BlockRepositoryImpl.kt
│   └── ResumeModuleConfiguration.kt
└── interfaces/rest/
    └── BlockController.kt
```

---

## 에러 코드

| 상황 | 에러코드 | HTTP |
|------|----------|------|
| 타인 블록 접근 | `FORBIDDEN` | 403 |
| 블록 없음 / 삭제됨 | `RESOURCE_NOT_FOUND` | 404 |
| 입력값 오류 | `VALIDATION_FAILED` | 400 |

---

## 테스트 전략

| 레이어 | 대상 | 방식 |
|--------|------|------|
| Application | CreateBlockUseCase, UpdateBlockUseCase, DeleteBlockUseCase, GetBlocksUseCase | MockK (Repository mock) |
| Interfaces | BlockController 4개 엔드포인트 | `@WebMvcTest` + MockK |
