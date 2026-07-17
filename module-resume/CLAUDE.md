# module-resume 설계 노트

> 이 모듈 작업 시작 전 이 파일을 먼저 읽는다.

## ⚠️ BlockType enum ↔ DB 컬럼 드리프트 (필독)

`blocks.type` 와 `block_drafts.block_type` 은 MySQL **네이티브 `enum(...)` 컬럼**이다.
Hibernate 6 이 `@Enumerated(EnumType.STRING)` 을 네이티브 enum 으로 생성하기 때문이다.

**`ddl-auto: update` 는 기존 enum 컬럼에 새 값을 추가하지 않는다.**
따라서 코드의 `BlockType` 에 값을 추가해도 DB 에는 반영되지 않으며, 누락된 값으로
블록을 생성하면 INSERT 가 `Data truncated for column 'type'` 로 거부되어 **500** 이 난다.

### BlockType 값을 추가/변경할 때 (필수 절차)

모든 환경(dev, prod)의 DB 에 직접 ALTER 를 실행한다. 코드 변경만으로는 끝나지 않는다.

```sql
ALTER TABLE blocks MODIFY COLUMN `type`
  enum('BASIC_INFO','SUMMARY','CAREER','INTRODUCTION','PROJECT','SKILL',
       'EDUCATION','CERTIFICATE','ACTIVITY','CUSTOM') NOT NULL;
ALTER TABLE block_drafts MODIFY COLUMN `block_type`
  enum('BASIC_INFO','SUMMARY','CAREER','INTRODUCTION','PROJECT','SKILL',
       'EDUCATION','CERTIFICATE','ACTIVITY','CUSTOM') NOT NULL;
```
(값 추가는 비파괴적 — 기존 행 보존.)

### 이력 (2026-06-21)

`BASIC_INFO`, `SUMMARY`, `INTRODUCTION` 3개가 DB enum 에서 누락돼 해당 타입 블록 생성이 500 이었다.
**dev DB 는 위 ALTER 적용 완료.** **prod DB 는 동일 드리프트 가능성 높음 → 적용 여부 확인 필요.**

## ⚠️ feedbacks.rating DECIMAL 마이그레이션 (2026-07-17, 미적용)

피드백 별점이 0.5 단위(기획 풀스펙)로 바뀌며 도메인 `rating: Double?` · 엔티티 `DECIMAL(2,1)` 이 됐다.
기존 컬럼은 `TINYINT` — **`ddl-auto: update` 는 컬럼 타입을 바꾸지 않으므로** 모든 환경(dev, prod) DB 에 직접 ALTER 를 실행해야 한다. 미적용 상태에서 0.5 단위 별점을 저장하면 반올림·오류가 난다.

```sql
-- 기존 정수 별점(1~5)은 1.0~5.0 으로 그대로 보존된다 (비파괴)
ALTER TABLE feedbacks MODIFY COLUMN rating DECIMAL(2,1) NULL;
```

`feedbacks.section`(VARCHAR(20), 섹션 피드백 대상)은 **신규 컬럼**이라 ddl-auto 가 추가해준다.
section 을 native enum 이 아닌 VARCHAR 로 고정한 이유 = 위 "BlockType enum ↔ DB 컬럼 드리프트" 함정 회피.

- **적용 현황: dev ❌ / prod ❌ (둘 다 미적용, 배포 시 실행 필요)**

## 트랜잭션 / 영속성 주의

`ResumeBlockJpaRepository.deleteAllByResumeId` 는 `@Modifying(flushAutomatically = true, clearAutomatically = true)` 이다.
`flushAutomatically` 가 빠지면, 같은 트랜잭션에서 먼저 merge 한 변경(예: 이력서 title 수정)이
bulk DELETE 의 `clearAutomatically` 에 의해 flush 전에 폐기된다 (200 응답인데 DB 미반영 버그).
bulk `@Modifying` 쿼리에는 항상 `flushAutomatically = true` 를 동반한다.
