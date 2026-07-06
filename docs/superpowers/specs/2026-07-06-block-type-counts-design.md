# 블록 타입별 개수 조회 API 설계

## 배경

블록 라이브러리 메인 화면(전체/기본정보/간단소개/경력/기술스택/프로젝트/자기소개/활동·교육 탭)에서
각 탭에 해당 타입의 블록 개수를 `(N)` 형태로 표시해야 한다. 현재는 이 개수를 조회하는 API가 없다.

## 엔드포인트

```
GET /api/blocks/counts
```

- 인증된 사용자(`resolveUserId`)의 블록을 타입별로 그룹핑해 개수를 반환한다.
- 소프트 삭제된 블록(`deletedAt IS NOT NULL`)은 집계에서 제외한다.
- 응답의 `counts` 순서는 `BlockType` enum 선언 순서로 고정한다.
- 데이터가 없는 타입도 `count: 0`으로 응답에 포함한다 (프론트 탭이 항상 전체 타입을 렌더링하기 때문).

## 응답 예시

```json
{
  "success": true,
  "data": {
    "totalCount": 12,
    "counts": [
      { "type": "BASIC_INFO", "count": 1 },
      { "type": "SUMMARY", "count": 2 },
      { "type": "CAREER", "count": 3 },
      { "type": "INTRODUCTION", "count": 0 },
      { "type": "PROJECT", "count": 4 },
      { "type": "SKILL", "count": 0 },
      { "type": "EDUCATION", "count": 0 },
      { "type": "CERTIFICATE", "count": 1 },
      { "type": "ACTIVITY", "count": 1 },
      { "type": "CUSTOM", "count": 0 }
    ]
  }
}
```

- `totalCount`는 `counts` 항목들의 합.
- 인증되지 않은 요청은 기존 블록 API와 동일하게 401(`UNAUTHORIZED`)을 반환한다.

## 레이어별 변경

| 레이어 | 변경 내용 |
|---|---|
| `domain/repository/BlockRepository.kt` | `fun countByUserId(userId: Long): Map<BlockType, Long>` 추가 (0건 타입은 맵에서 생략 — 채우는 책임은 UseCase) |
| `infrastructure/persistence/BlockJpaRepository.kt` | `@Query`로 `WHERE userId = :userId AND deletedAt IS NULL GROUP BY type` 집계, 인터페이스 프로젝션(`BlockTypeCountView`)으로 반환 |
| `infrastructure/persistence/BlockRepositoryImpl.kt` | 프로젝션 결과 리스트 → `Map<BlockType, Long>` 변환 (`associate`) |
| `application/usecase/GetBlockCountsUseCase.kt` (신규) | `BlockType.entries` 전체를 순회하며 맵에 없는 타입은 0으로 채움, `totalCount` 계산. 읽기 전용 쿼리이므로 `GetBlocksUseCase`와 동일하게 `@Transactional` 미부착 |
| `interfaces/rest/BlockController.kt` | `GET /counts` 액션 + `BlockCountsResponse`/`BlockTypeCountResponse` DTO 추가 |

## 테스트 계획

- `GetBlockCountsUseCaseTest`
  - 일부 타입에만 블록이 있는 경우 → 나머지 타입은 0, `totalCount`는 합계와 일치
  - 블록이 하나도 없는 경우 → 모든 타입 0, `totalCount` 0
- `BlockControllerTest`
  - 인증된 사용자 → 200 응답, `ApiResponse.ok` 래핑 확인
  - 미인증 요청 → 401 (`UNAUTHORIZED`)

## 범위 밖

- 리소스별(이력서별) 블록 개수 집계는 다루지 않는다 — 이 API는 기존 `GET /api/blocks`와 동일하게 사용자 전체 블록 라이브러리 기준이다.
- 페이지네이션/정렬 파라미터는 없다 (개수만 반환).
