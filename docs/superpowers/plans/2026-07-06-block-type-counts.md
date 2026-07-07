# 블록 타입별 개수 조회 API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 로그인한 사용자의 블록을 타입별로 그룹핑해 개수를 반환하는 `GET /api/blocks/counts` API를 추가한다.

**Architecture:** 기존 `module-resume`의 Block 관련 4계층(domain → application → infrastructure → interfaces) 구조를 그대로 따른다. JPQL `GROUP BY` 집계 쿼리 + 인터페이스 프로젝션으로 DB에서 타입별 개수를 가져오고, UseCase에서 `BlockType.entries` 전체를 순회하며 0건 타입을 채운다.

**Tech Stack:** Kotlin, Spring Boot, Spring Data JPA(`@Query` + 인터페이스 프로젝션), MockK, `@WebMvcTest`

## Global Constraints

- 응답은 `ApiResponse<T>` 공통 래퍼 사용 (`doc/API_RESPONSE.md`)
- 소프트 삭제된 블록(`deletedAt IS NOT NULL`)은 집계에서 제외
- `counts` 배열 순서는 `BlockType` enum 선언 순서 고정, 데이터 없는 타입도 `count: 0`으로 포함
- 읽기 전용 쿼리 UseCase는 `@Transactional` 미부착 (기존 `GetBlocksUseCase` 패턴과 동일)
- 클래스 네이밍은 `doc/discussion.md` [12] 컨벤션 준수 (`UseCase`, `Repository`, `Request`/`Response` 등)
- 테스트 네이밍은 한글 back-tick 문장 (`` `...한다` ``) 형식, MockK 사용

---

### Task 1: Domain — `BlockRepository`에 count 메서드 추가

**Files:**
- Modify: `module-resume/src/main/kotlin/com/atomiccv/resume/domain/repository/BlockRepository.kt`

**Interfaces:**
- Produces: `BlockRepository.countByUserId(userId: Long): Map<BlockType, Long>` — Task 2(인프라 구현), Task 3(UseCase)에서 사용

이 태스크는 인터페이스 시그니처만 추가하는 단계라 별도 테스트 없이 컴파일만 확인한다(다음 태스크에서 구현체가 이 시그니처를 구현하면서 실질적으로 검증됨).

- [ ] **Step 1: `BlockRepository`에 메서드 추가**

`module-resume/src/main/kotlin/com/atomiccv/resume/domain/repository/BlockRepository.kt` 전체를 다음으로 교체:

```kotlin
package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.domain.Page

interface BlockRepository {
    fun save(block: Block): Block

    fun saveAll(blocks: List<Block>): List<Block>

    fun findById(id: Long): Block?

    fun findPageByUserId(
        userId: Long,
        page: Int,
        size: Int,
    ): Page<Block>

    fun findPageByUserIdAndType(
        userId: Long,
        type: BlockType,
        page: Int,
        size: Int,
    ): Page<Block>

    fun countByUserId(userId: Long): Map<BlockType, Long>
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :module-resume:compileKotlin`
Expected: `BlockRepositoryImpl`이 아직 새 메서드를 구현하지 않아 컴파일 실패 (`class BlockRepositoryImpl is not abstract and does not implement member`) — 이 실패는 정상이며 Task 2에서 해소된다.

- [ ] **Step 3: Commit**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/domain/repository/BlockRepository.kt
git commit -m "feat(resume): BlockRepository에 타입별 카운트 조회 메서드 추가"
```

---

### Task 2: Infrastructure — JPQL 집계 쿼리 + Repository 구현체

**Files:**
- Modify: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockJpaRepository.kt`
- Modify: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockRepositoryImpl.kt`

**Interfaces:**
- Consumes: `BlockRepository.countByUserId(userId: Long): Map<BlockType, Long>` (Task 1에서 정의)
- Produces: `BlockJpaRepository.countByUserIdGroupedByType(userId: Long): List<BlockTypeCountProjection>`, `BlockTypeCountProjection { val type: BlockType; val count: Long }` — 같은 파일 내에서만 쓰이므로 다른 태스크는 직접 참조하지 않음

이 계층은 실제 DB 접근 코드라 별도 슬라이스 테스트 없이(기존 `BlockRepositoryImpl`도 단위 테스트가 없음 — 리포지토리 구현체는 `@DataJpaTest`/통합 테스트 대상 밖) Task 3의 UseCase 테스트에서 목(mock) 처리된 `BlockRepository`로 간접 검증한다.

- [ ] **Step 1: `BlockJpaRepository`에 집계 쿼리 추가**

`module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockJpaRepository.kt` 전체를 다음으로 교체:

```kotlin
package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BlockJpaRepository : JpaRepository<BlockJpaEntity, Long> {
    fun findAllByUserIdAndDeletedAtIsNull(
        userId: Long,
        pageable: Pageable,
    ): Page<BlockJpaEntity>

    fun findAllByUserIdAndTypeAndDeletedAtIsNull(
        userId: Long,
        type: BlockType,
        pageable: Pageable,
    ): Page<BlockJpaEntity>

    @Query(
        """
        SELECT b.type AS type,
               COUNT(b) AS count
        FROM BlockJpaEntity b
        WHERE b.userId = :userId
          AND b.deletedAt IS NULL
        GROUP BY b.type
    """,
    )
    fun countByUserIdGroupedByType(
        @Param("userId") userId: Long,
    ): List<BlockTypeCountProjection>
}

interface BlockTypeCountProjection {
    val type: BlockType
    val count: Long
}
```

- [ ] **Step 2: `BlockRepositoryImpl`에 구현 추가**

`module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockRepositoryImpl.kt` 전체를 다음으로 교체:

```kotlin
package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class BlockRepositoryImpl(
    private val jpaRepository: BlockJpaRepository,
) : BlockRepository {
    override fun save(block: Block): Block = jpaRepository.save(BlockJpaEntity.fromDomain(block)).toDomain()

    override fun saveAll(blocks: List<Block>): List<Block> =
        jpaRepository.saveAll(blocks.map { BlockJpaEntity.fromDomain(it) }).map { it.toDomain() }

    override fun findById(id: Long): Block? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findPageByUserId(
        userId: Long,
        page: Int,
        size: Int,
    ): Page<Block> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable).map { it.toDomain() }
    }

    override fun findPageByUserIdAndType(
        userId: Long,
        type: BlockType,
        page: Int,
        size: Int,
    ): Page<Block> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return jpaRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(userId, type, pageable).map { it.toDomain() }
    }

    override fun countByUserId(userId: Long): Map<BlockType, Long> =
        jpaRepository.countByUserIdGroupedByType(userId).associate { it.type to it.count }
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew :module-resume:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockJpaRepository.kt module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockRepositoryImpl.kt
git commit -m "feat(resume): 블록 타입별 개수 집계 쿼리 구현"
```

---

### Task 3: Application — `GetBlockCountsUseCase` (TDD)

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockCountsUseCase.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockCountsUseCaseTest.kt`

**Interfaces:**
- Consumes: `BlockRepository.countByUserId(userId: Long): Map<BlockType, Long>` (Task 1/2)
- Produces:
  - `data class BlockTypeCount(val type: BlockType, val count: Long)`
  - `data class BlockCounts(val totalCount: Long, val counts: List<BlockTypeCount>)`
  - `class GetBlockCountsUseCase(private val blockRepository: BlockRepository) { fun getCounts(userId: Long): BlockCounts }`
  - Task 4(Controller)가 이 타입들을 그대로 사용

- [ ] **Step 1: 실패하는 테스트 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockCountsUseCaseTest.kt` 생성:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetBlockCountsUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = GetBlockCountsUseCase(blockRepository)

    @Test
    fun `일부 타입에만 블록이 있으면 나머지 타입은 0건으로 채워 전체 타입을 반환한다`() {
        every { blockRepository.countByUserId(10L) } returns
            mapOf(
                BlockType.CAREER to 3L,
                BlockType.PROJECT to 2L,
            )

        val result = useCase.getCounts(10L)

        assertEquals(BlockType.entries.size, result.counts.size)
        assertEquals(5L, result.totalCount)
        assertEquals(3L, result.counts.first { it.type == BlockType.CAREER }.count)
        assertEquals(2L, result.counts.first { it.type == BlockType.PROJECT }.count)
        assertEquals(0L, result.counts.first { it.type == BlockType.SKILL }.count)
        verify { blockRepository.countByUserId(10L) }
    }

    @Test
    fun `블록이 하나도 없으면 모든 타입이 0건이고 totalCount도 0이다`() {
        every { blockRepository.countByUserId(10L) } returns emptyMap()

        val result = useCase.getCounts(10L)

        assertEquals(0L, result.totalCount)
        assertEquals(BlockType.entries.size, result.counts.size)
        assertEquals(true, result.counts.all { it.count == 0L })
    }

    @Test
    fun `counts 순서는 BlockType enum 선언 순서를 따른다`() {
        every { blockRepository.countByUserId(10L) } returns emptyMap()

        val result = useCase.getCounts(10L)

        assertEquals(BlockType.entries, result.counts.map { it.type })
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `./gradlew :module-resume:test --tests "com.atomiccv.resume.application.usecase.GetBlockCountsUseCaseTest"`
Expected: FAIL — `GetBlockCountsUseCase`, `BlockCounts`, `BlockTypeCount` 클래스가 없어 컴파일 실패

- [ ] **Step 3: 최소 구현 작성**

`module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockCountsUseCase.kt` 생성:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository

data class BlockTypeCount(
    val type: BlockType,
    val count: Long,
)

data class BlockCounts(
    val totalCount: Long,
    val counts: List<BlockTypeCount>,
)

class GetBlockCountsUseCase(
    private val blockRepository: BlockRepository,
) {
    fun getCounts(userId: Long): BlockCounts {
        val countsByType = blockRepository.countByUserId(userId)
        val counts = BlockType.entries.map { type -> BlockTypeCount(type, countsByType[type] ?: 0L) }
        return BlockCounts(totalCount = counts.sumOf { it.count }, counts = counts)
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :module-resume:test --tests "com.atomiccv.resume.application.usecase.GetBlockCountsUseCaseTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 5: Commit**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockCountsUseCase.kt module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockCountsUseCaseTest.kt
git commit -m "feat(resume): GetBlockCountsUseCase 구현"
```

---

### Task 4: Interfaces — `GET /api/blocks/counts` 컨트롤러 (TDD)

**Files:**
- Modify: `module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockController.kt`
- Modify: `module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockControllerTest.kt`

**Interfaces:**
- Consumes: `GetBlockCountsUseCase.getCounts(userId: Long): BlockCounts`, `BlockCounts { totalCount: Long, counts: List<BlockTypeCount> }`, `BlockTypeCount { type: BlockType, count: Long }` (Task 3)
- Produces: `GET /api/blocks/counts` — 최종 사용자(FE)가 소비하는 엔드포인트, 이후 태스크 없음

- [ ] **Step 1: 실패하는 컨트롤러 테스트 추가**

`module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockControllerTest.kt`을 열어 import 블록에 `GetBlockCountsUseCase`, `BlockCounts`, `BlockTypeCount`를 추가하고, `MockConfig`에 빈을 추가하고, 클래스에 `@Autowired lateinit var getBlockCountsUseCase: GetBlockCountsUseCase`를 추가한 뒤, 클래스 마지막 `}` 앞에 아래 두 테스트를 추가한다.

`import` 섹션 (기존 import 아래에 추가):
```kotlin
import com.atomiccv.resume.application.usecase.BlockCounts
import com.atomiccv.resume.application.usecase.BlockTypeCount
import com.atomiccv.resume.application.usecase.GetBlockCountsUseCase
```

클래스 필드 (기존 `getBlocksUseCase` 필드 아래에 추가):
```kotlin
    @Autowired
    lateinit var getBlockCountsUseCase: GetBlockCountsUseCase
```

`MockConfig` (기존 `getBlocksUseCase()` 빈 아래에 추가):
```kotlin
        @Bean
        fun getBlockCountsUseCase(): GetBlockCountsUseCase = mockk()
```

클래스 마지막(`DELETE` 테스트 다음, 파일 끝 `}` 앞)에 테스트 추가:
```kotlin
    @Test
    @WithMockUser(username = "1")
    fun `GET api-blocks-counts - 타입별 블록 개수를 반환한다`() {
        val counts =
            BlockCounts(
                totalCount = 3L,
                counts =
                    listOf(
                        BlockTypeCount(BlockType.BASIC_INFO, 0L),
                        BlockTypeCount(BlockType.CAREER, 3L),
                    ),
            )
        every { getBlockCountsUseCase.getCounts(1L) } returns counts

        mockMvc.get("/api/blocks/counts").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.totalCount") { value(3) }
            jsonPath("$.data.counts[0].type") { value("BASIC_INFO") }
            jsonPath("$.data.counts[0].count") { value(0) }
            jsonPath("$.data.counts[1].type") { value("CAREER") }
            jsonPath("$.data.counts[1].count") { value(3) }
        }
    }

    @Test
    fun `GET api-blocks-counts - 미인증 요청 시 401을 반환한다`() {
        mockMvc.get("/api/blocks/counts").andExpect {
            status { isUnauthorized() }
        }
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `./gradlew :module-resume:test --tests "com.atomiccv.resume.interfaces.rest.BlockControllerTest"`
Expected: FAIL — `GetBlockCountsUseCase` 빈 미등록 또는 404(`/counts` 경로 없음)

- [ ] **Step 3: 컨트롤러에 엔드포인트 추가**

`module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockController.kt`에서:

1. 생성자에 `private val getBlockCountsUseCase: GetBlockCountsUseCase,` 추가 (기존 `objectMapper` 파라미터 앞).
2. import 블록에 추가:
```kotlin
import com.atomiccv.resume.application.usecase.GetBlockCountsUseCase
```
3. `getBlocks` 메서드 다음, `createBlocks` 메서드 이전에 아래 메서드 삽입:

```kotlin
    @Operation(
        summary = "블록 타입별 개수 조회",
        description = "로그인한 사용자의 블록을 타입별로 그룹핑한 개수를 반환합니다. 데이터가 없는 타입도 0건으로 포함됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청 (UNAUTHORIZED)",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(ref = "#/components/schemas/ApiResponse"),
                    examples = [ExampleObject(value = """{"success":false,"message":"인증이 필요합니다"}""")],
                ),
            ],
        ),
    )
    @GetMapping("/counts")
    fun getBlockCounts(authentication: Authentication): ResponseEntity<ApiResponse<BlockCountsResponse>> {
        val userId = resolveUserId(authentication)
        val result = getBlockCountsUseCase.getCounts(userId)
        return ResponseEntity.ok(ApiResponse.ok(result.toResponse()))
    }
```

4. 파일 하단 DTO 섹션(`BlockResponse`/`toResponse` 근처)에 아래 DTO와 변환 함수 추가:

```kotlin
@Schema(description = "블록 타입별 개수 응답")
data class BlockCountsResponse(
    @Schema(description = "전체 블록 개수", example = "12")
    val totalCount: Long,
    @Schema(description = "타입별 블록 개수 목록")
    val counts: List<BlockTypeCountResponse>,
)

@Schema(description = "블록 타입별 개수")
data class BlockTypeCountResponse(
    @Schema(description = "블록 타입", example = "CAREER")
    val type: BlockType,
    @Schema(description = "개수", example = "3")
    val count: Long,
)

fun BlockCounts.toResponse() =
    BlockCountsResponse(
        totalCount = totalCount,
        counts = counts.map { BlockTypeCountResponse(type = it.type, count = it.count) },
    )
```

**주의:** `@GetMapping("/counts")`는 반드시 `@GetMapping`(목록 조회, path variable 없음) 다음에 위치시킨다. Spring MVC는 HTTP 메서드+경로 조합으로 매칭하므로 `GET /api/blocks`와 `GET /api/blocks/counts`는 충돌하지 않지만, 컨트롤러 가독성을 위해 관련 메서드를 인접시킨다.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :module-resume:test --tests "com.atomiccv.resume.interfaces.rest.BlockControllerTest"`
Expected: BUILD SUCCESSFUL, 전체 테스트 통과 (기존 4개 + 신규 2개 = 6개)

- [ ] **Step 5: 모듈 전체 테스트 실행**

Run: `./gradlew :module-resume:test`
Expected: BUILD SUCCESSFUL (기존 테스트 회귀 없음)

- [ ] **Step 6: ktlint/detekt 검사**

Run: `./gradlew ktlintCheck detekt`
Expected: BUILD SUCCESSFUL — 위반 시 `./gradlew ktlintFormat` 실행 후 재검사

- [ ] **Step 7: Commit**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockController.kt module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockControllerTest.kt
git commit -m "feat(resume): GET /api/blocks/counts 엔드포인트 추가"
```

---

### Task 5: `doc/TASKS.md` 업데이트 및 최종 검증

**Files:**
- Modify: `doc/TASKS.md` (해당 항목이 있다면 🟢로 갱신 — 없다면 이 태스크는 스킵하고 다음 단계로 넘어간다)

- [ ] **Step 1: `doc/TASKS.md`에서 블록 관련 항목 확인**

Run: `grep -n "블록" doc/TASKS.md`

- 관련 항목이 있으면 상태를 🟢로 변경하고 커밋한다.
- 관련 항목이 없으면 (이 기능이 TASKS.md에 트래킹되지 않는 애드혹 요청이면) 이 스텝은 건너뛴다.

- [ ] **Step 2: 전체 빌드 최종 확인**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: (조건부) Commit**

```bash
git add doc/TASKS.md
git commit -m "docs: 블록 타입별 개수 API 작업 완료 반영"
```
