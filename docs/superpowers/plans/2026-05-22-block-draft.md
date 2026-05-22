# Block Draft (임시저장) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 블록 임시저장 도메인을 구현한다 — 생성·조회·수정·삭제·만료 스케줄러·REST API 포함.

**Architecture:** `module-resume` 내에 `BlockDraft` 도메인을 추가한다. Hexagonal 아키텍처를 따르며 domain → repository interface → JPA infrastructure → UseCase → Controller 순으로 구현한다. "작성중" 배지는 DB 컬럼이 아닌 응답 계산으로 처리한다.

**Tech Stack:** Kotlin, Spring Boot 3.x, Spring Data JPA, MockK (테스트), @Scheduled (스케줄러)

**Spec:** `docs/superpowers/specs/2026-05-22-block-draft-design.md`

---

## 파일 구조

**신규 생성**

| 파일 | 역할 |
|------|------|
| `module-shared/.../exception/ErrorCode.kt` | 에러 코드 3개 추가 |
| `module-resume/.../domain/model/BlockDraft.kt` | 도메인 모델 |
| `module-resume/.../domain/repository/BlockDraftRepository.kt` | 리포지터리 인터페이스 |
| `module-resume/.../infrastructure/persistence/BlockDraftJpaEntity.kt` | JPA 엔티티 |
| `module-resume/.../infrastructure/persistence/BlockDraftJpaRepository.kt` | Spring Data JPA 인터페이스 |
| `module-resume/.../infrastructure/persistence/BlockDraftRepositoryImpl.kt` | 리포지터리 구현체 |
| `module-resume/.../application/usecase/CreateBlockDraftUseCase.kt` | 생성 |
| `module-resume/.../application/usecase/UpdateBlockDraftUseCase.kt` | 덮어쓰기 |
| `module-resume/.../application/usecase/GetBlockDraftsUseCase.kt` | 목록 조회 |
| `module-resume/.../application/usecase/GetBlockDraftUseCase.kt` | 단건 조회 |
| `module-resume/.../application/usecase/DeleteBlockDraftUseCase.kt` | 단건 삭제 |
| `module-resume/.../application/usecase/DeleteBlockDraftsUseCase.kt` | 선택 삭제 |
| `module-resume/.../application/usecase/DeleteAllBlockDraftsByTypeUseCase.kt` | 타입별 전체 삭제 |
| `module-resume/.../infrastructure/scheduler/BlockDraftExpireScheduler.kt` | 만료 스케줄러 |
| `module-resume/.../interfaces/rest/BlockDraftController.kt` | REST 컨트롤러 |

**수정**

| 파일 | 변경 내용 |
|------|---------|
| `module-resume/.../infrastructure/ResumeModuleConfiguration.kt` | 7개 UseCase Bean 등록 |

---

## Task 1: 도메인 모델 & 에러 코드

**Files:**
- Modify: `module-shared/src/main/kotlin/com/atomiccv/shared/common/exception/ErrorCode.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/domain/model/BlockDraft.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/domain/repository/BlockDraftRepository.kt`

- [ ] **Step 1: ErrorCode에 임시저장 에러 코드 3개 추가**

`module-shared/src/main/kotlin/com/atomiccv/shared/common/exception/ErrorCode.kt`의 기존 enum 항목 뒤에 추가한다:

```kotlin
BLOCK_DRAFT_TITLE_REQUIRED(400, "BLOCK_DRAFT_TITLE_REQUIRED", "제목을 입력해주세요"),
BLOCK_DRAFT_NOT_FOUND(404, "BLOCK_DRAFT_NOT_FOUND", "임시저장을 찾을 수 없습니다"),
BLOCK_DRAFT_FORBIDDEN(403, "BLOCK_DRAFT_FORBIDDEN", "임시저장에 접근 권한이 없습니다"),
```

- [ ] **Step 2: BlockDraft 도메인 모델 작성**

`module-resume/src/main/kotlin/com/atomiccv/resume/domain/model/BlockDraft.kt`:

```kotlin
package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

data class BlockDraft(
    val id: Long = 0,
    val userId: Long,
    val blockType: BlockType,
    val title: String,
    val contentJson: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isOwnedBy(ownerId: Long): Boolean = userId == ownerId

    companion object {
        const val EXPIRY_DAYS = 14L
    }
}
```

- [ ] **Step 3: BlockDraftRepository 인터페이스 작성**

`module-resume/src/main/kotlin/com/atomiccv/resume/domain/repository/BlockDraftRepository.kt`:

```kotlin
package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import java.time.LocalDateTime

interface BlockDraftRepository {
    fun save(draft: BlockDraft): BlockDraft

    fun findById(id: Long): BlockDraft?

    fun findAllByUserIdAndBlockType(userId: Long, blockType: BlockType): List<BlockDraft>

    fun findAllByIds(ids: List<Long>): List<BlockDraft>

    fun deleteById(id: Long)

    fun deleteAllByIds(ids: List<Long>)

    fun deleteAllByUserIdAndBlockType(userId: Long, blockType: BlockType)

    fun deleteAllExpiredBefore(threshold: LocalDateTime): Int
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew :module-resume:compileKotlin :module-shared:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add module-shared/src/main/kotlin/com/atomiccv/shared/common/exception/ErrorCode.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/domain/model/BlockDraft.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/domain/repository/BlockDraftRepository.kt
git commit -m "feat(resume): BlockDraft 도메인 모델 및 에러 코드 추가"
```

---

## Task 2: JPA 인프라스트럭처

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftJpaEntity.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftJpaRepository.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftRepositoryImpl.kt`

- [ ] **Step 1: BlockDraftJpaEntity 작성**

`module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftJpaEntity.kt`:

```kotlin
package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.infrastructure.persistence.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "block_drafts")
class BlockDraftJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false)
    val blockType: BlockType,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(name = "content_json", columnDefinition = "JSON")
    val contentJson: String?,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
) : BaseJpaEntity() {
    fun toDomain() =
        BlockDraft(
            id = id,
            userId = userId,
            blockType = blockType,
            title = title,
            contentJson = contentJson ?: "{}",
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun fromDomain(draft: BlockDraft) =
            BlockDraftJpaEntity(
                id = draft.id,
                userId = draft.userId,
                blockType = draft.blockType,
                title = draft.title,
                contentJson = draft.contentJson,
                expiresAt = draft.expiresAt,
            )
    }
}
```

- [ ] **Step 2: BlockDraftJpaRepository 작성**

`module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftJpaRepository.kt`:

```kotlin
package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface BlockDraftJpaRepository : JpaRepository<BlockDraftJpaEntity, Long> {
    fun findAllByUserIdAndBlockTypeOrderByUpdatedAtDesc(
        userId: Long,
        blockType: BlockType,
    ): List<BlockDraftJpaEntity>

    @Modifying
    @Query("DELETE FROM BlockDraftJpaEntity b WHERE b.userId = :userId AND b.blockType = :blockType")
    fun deleteAllByUserIdAndBlockType(userId: Long, blockType: BlockType)

    @Modifying
    @Query("DELETE FROM BlockDraftJpaEntity b WHERE b.expiresAt < :threshold")
    fun deleteAllExpiredBefore(threshold: LocalDateTime): Int
}
```

- [ ] **Step 3: BlockDraftRepositoryImpl 작성**

`module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftRepositoryImpl.kt`:

```kotlin
package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BlockDraftRepositoryImpl(
    private val jpaRepository: BlockDraftJpaRepository,
) : BlockDraftRepository {
    override fun save(draft: BlockDraft): BlockDraft =
        jpaRepository.save(BlockDraftJpaEntity.fromDomain(draft)).toDomain()

    override fun findById(id: Long): BlockDraft? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllByUserIdAndBlockType(userId: Long, blockType: BlockType): List<BlockDraft> =
        jpaRepository.findAllByUserIdAndBlockTypeOrderByUpdatedAtDesc(userId, blockType).map { it.toDomain() }

    override fun findAllByIds(ids: List<Long>): List<BlockDraft> =
        jpaRepository.findAllById(ids).map { it.toDomain() }

    override fun deleteById(id: Long) = jpaRepository.deleteById(id)

    override fun deleteAllByIds(ids: List<Long>) = jpaRepository.deleteAllByIdInBatch(ids)

    override fun deleteAllByUserIdAndBlockType(userId: Long, blockType: BlockType) =
        jpaRepository.deleteAllByUserIdAndBlockType(userId, blockType)

    override fun deleteAllExpiredBefore(threshold: LocalDateTime): Int =
        jpaRepository.deleteAllExpiredBefore(threshold)
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew :module-resume:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftJpaEntity.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftJpaRepository.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockDraftRepositoryImpl.kt
git commit -m "feat(resume): BlockDraft JPA 인프라스트럭처 추가"
```

---

## Task 3: Create & Update UseCase

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/CreateBlockDraftUseCase.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockDraftUseCase.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/CreateBlockDraftUseCaseTest.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockDraftUseCaseTest.kt`

- [ ] **Step 1: CreateBlockDraftUseCase 테스트 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/CreateBlockDraftUseCaseTest.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateBlockDraftUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = CreateBlockDraftUseCase(blockDraftRepository)

    private val saved = BlockDraft(
        id = 1L,
        userId = 10L,
        blockType = BlockType.CAREER,
        title = "카카오 백엔드 개발자",
        contentJson = """{"company":"카카오"}""",
        expiresAt = LocalDateTime.now().plusDays(14),
    )

    @Test
    fun `임시저장 생성 시 userId·blockType·title·contentJson이 저장되고 반환된다`() {
        val command = CreateBlockDraftCommand(
            userId = 10L,
            blockType = BlockType.CAREER,
            title = "카카오 백엔드 개발자",
            contentJson = """{"company":"카카오"}""",
        )
        every { blockDraftRepository.save(any()) } returns saved

        val result = useCase.create(command)

        assertEquals(1L, result.id)
        verify {
            blockDraftRepository.save(match {
                it.userId == 10L &&
                    it.blockType == BlockType.CAREER &&
                    it.title == "카카오 백엔드 개발자"
            })
        }
    }

    @Test
    fun `생성 시 expiresAt은 현재 시각 기준 14일 후로 설정된다`() {
        val command = CreateBlockDraftCommand(
            userId = 10L, blockType = BlockType.CAREER,
            title = "제목", contentJson = "{}",
        )
        every { blockDraftRepository.save(any()) } answers { firstArg() }

        val result = useCase.create(command)

        assertTrue(result.expiresAt.isAfter(LocalDateTime.now().plusDays(13)))
    }

    @Test
    fun `title이 빈 문자열이면 BLOCK_DRAFT_TITLE_REQUIRED 예외가 발생한다`() {
        val command = CreateBlockDraftCommand(
            userId = 10L, blockType = BlockType.CAREER,
            title = "   ", contentJson = "{}",
        )

        val ex = assertFailsWith<BusinessException> { useCase.create(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_TITLE_REQUIRED, ex.errorCode)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :module-resume:test --tests "com.atomiccv.resume.application.usecase.CreateBlockDraftUseCaseTest"
```

Expected: FAIL (CreateBlockDraftUseCase 클래스 없음)

- [ ] **Step 3: CreateBlockDraftUseCase 구현**

`module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/CreateBlockDraftUseCase.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class CreateBlockDraftCommand(
    val userId: Long,
    val blockType: BlockType,
    val title: String,
    val contentJson: String,
)

@Transactional
class CreateBlockDraftUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun create(command: CreateBlockDraftCommand): BlockDraft {
        if (command.title.isBlank()) throw BusinessException(ErrorCode.BLOCK_DRAFT_TITLE_REQUIRED)
        return blockDraftRepository.save(
            BlockDraft(
                userId = command.userId,
                blockType = command.blockType,
                title = command.title,
                contentJson = command.contentJson,
                expiresAt = LocalDateTime.now().plusDays(BlockDraft.EXPIRY_DAYS),
            ),
        )
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :module-resume:test --tests "com.atomiccv.resume.application.usecase.CreateBlockDraftUseCaseTest"
```

Expected: PASS (3개)

- [ ] **Step 5: UpdateBlockDraftUseCase 테스트 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockDraftUseCaseTest.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UpdateBlockDraftUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = UpdateBlockDraftUseCase(blockDraftRepository)

    private val existingDraft = BlockDraft(
        id = 1L,
        userId = 10L,
        blockType = BlockType.CAREER,
        title = "원본 제목",
        contentJson = "{}",
        expiresAt = LocalDateTime.now().plusDays(14),
    )

    @Test
    fun `덮어쓰기 시 title·contentJson이 갱신되고 expiresAt이 연장된다`() {
        val command = UpdateBlockDraftCommand(
            draftId = 1L, userId = 10L,
            title = "수정 제목", contentJson = """{"company":"네이버"}""",
        )
        every { blockDraftRepository.findById(1L) } returns existingDraft
        every { blockDraftRepository.save(any()) } answers { firstArg() }

        val result = useCase.update(command)

        assertEquals("수정 제목", result.title)
        assertTrue(result.expiresAt.isAfter(LocalDateTime.now().plusDays(13)))
        verify { blockDraftRepository.save(match { it.title == "수정 제목" }) }
    }

    @Test
    fun `존재하지 않는 드래프트 수정 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        every { blockDraftRepository.findById(999L) } returns null
        val command = UpdateBlockDraftCommand(
            draftId = 999L, userId = 10L, title = "제목", contentJson = "{}",
        )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `만료된 드래프트 수정 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        val expiredDraft = existingDraft.copy(expiresAt = LocalDateTime.now().minusDays(1))
        every { blockDraftRepository.findById(1L) } returns expiredDraft
        val command = UpdateBlockDraftCommand(
            draftId = 1L, userId = 10L, title = "제목", contentJson = "{}",
        )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 드래프트 수정 시 BLOCK_DRAFT_FORBIDDEN 예외가 발생한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft
        val command = UpdateBlockDraftCommand(
            draftId = 1L, userId = 99L, title = "제목", contentJson = "{}",
        )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `title이 빈 문자열이면 BLOCK_DRAFT_TITLE_REQUIRED 예외가 발생한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft
        val command = UpdateBlockDraftCommand(
            draftId = 1L, userId = 10L, title = "   ", contentJson = "{}",
        )

        val ex = assertFailsWith<BusinessException> { useCase.update(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_TITLE_REQUIRED, ex.errorCode)
    }
}
```

- [ ] **Step 6: UpdateBlockDraftUseCase 구현**

`module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockDraftUseCase.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class UpdateBlockDraftCommand(
    val draftId: Long,
    val userId: Long,
    val title: String,
    val contentJson: String,
)

@Transactional
class UpdateBlockDraftUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun update(command: UpdateBlockDraftCommand): BlockDraft {
        if (command.title.isBlank()) throw BusinessException(ErrorCode.BLOCK_DRAFT_TITLE_REQUIRED)
        val draft = findActiveDraft(command.draftId)
        if (!draft.isOwnedBy(command.userId)) throw BusinessException(ErrorCode.BLOCK_DRAFT_FORBIDDEN)
        return blockDraftRepository.save(
            draft.copy(
                title = command.title,
                contentJson = command.contentJson,
                expiresAt = LocalDateTime.now().plusDays(BlockDraft.EXPIRY_DAYS),
            ),
        )
    }

    private fun findActiveDraft(draftId: Long): BlockDraft {
        val draft = blockDraftRepository.findById(draftId)
            ?: throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        if (draft.isExpired()) throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        return draft
    }
}
```

- [ ] **Step 7: 전체 테스트 통과 확인**

```bash
./gradlew :module-resume:test --tests "com.atomiccv.resume.application.usecase.CreateBlockDraftUseCaseTest" \
  --tests "com.atomiccv.resume.application.usecase.UpdateBlockDraftUseCaseTest"
```

Expected: PASS (8개)

- [ ] **Step 8: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/CreateBlockDraftUseCase.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockDraftUseCase.kt \
  module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/CreateBlockDraftUseCaseTest.kt \
  module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockDraftUseCaseTest.kt
git commit -m "feat(resume): CreateBlockDraftUseCase·UpdateBlockDraftUseCase 구현"
```

---

## Task 4: Get UseCases

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftsUseCase.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftUseCase.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftsUseCaseTest.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftUseCaseTest.kt`

- [ ] **Step 1: GetBlockDraftsUseCase 테스트 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftsUseCaseTest.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetBlockDraftsUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = GetBlockDraftsUseCase(blockDraftRepository)

    private fun draft(id: Long, expiresAt: LocalDateTime = LocalDateTime.now().plusDays(14)) =
        BlockDraft(
            id = id, userId = 10L, blockType = BlockType.CAREER,
            title = "제목 $id", contentJson = "{}",
            expiresAt = expiresAt,
        )

    @Test
    fun `목록 조회 시 만료되지 않은 드래프트만 반환된다`() {
        val active = draft(1L)
        val expired = draft(2L, expiresAt = LocalDateTime.now().minusDays(1))
        every { blockDraftRepository.findAllByUserIdAndBlockType(10L, BlockType.CAREER) } returns listOf(active, expired)

        val result = useCase.getDrafts(GetBlockDraftsQuery(userId = 10L, blockType = BlockType.CAREER, currentDraftId = null))

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `currentDraftId와 일치하는 항목은 isActive가 true다`() {
        every { blockDraftRepository.findAllByUserIdAndBlockType(10L, BlockType.CAREER) } returns listOf(draft(1L), draft(2L))

        val result = useCase.getDrafts(GetBlockDraftsQuery(userId = 10L, blockType = BlockType.CAREER, currentDraftId = 1L))

        assertTrue(result.find { it.id == 1L }!!.isActive)
        assertFalse(result.find { it.id == 2L }!!.isActive)
    }

    @Test
    fun `currentDraftId가 null이면 모든 항목의 isActive가 false다`() {
        every { blockDraftRepository.findAllByUserIdAndBlockType(10L, BlockType.CAREER) } returns listOf(draft(1L))

        val result = useCase.getDrafts(GetBlockDraftsQuery(userId = 10L, blockType = BlockType.CAREER, currentDraftId = null))

        assertFalse(result[0].isActive)
    }
}
```

- [ ] **Step 2: GetBlockDraftsUseCase 구현**

`module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftsUseCase.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import java.time.LocalDateTime

data class GetBlockDraftsQuery(
    val userId: Long,
    val blockType: BlockType,
    val currentDraftId: Long?,
)

data class BlockDraftSummary(
    val id: Long,
    val title: String,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
)

class GetBlockDraftsUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun getDrafts(query: GetBlockDraftsQuery): List<BlockDraftSummary> =
        blockDraftRepository.findAllByUserIdAndBlockType(query.userId, query.blockType)
            .filter { !it.isExpired() }
            .map { draft ->
                BlockDraftSummary(
                    id = draft.id,
                    title = draft.title,
                    updatedAt = draft.updatedAt,
                    isActive = draft.id == query.currentDraftId,
                )
            }
}
```

- [ ] **Step 3: GetBlockDraftUseCase 테스트 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftUseCaseTest.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetBlockDraftUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = GetBlockDraftUseCase(blockDraftRepository)

    private val existingDraft = BlockDraft(
        id = 1L, userId = 10L, blockType = BlockType.CAREER,
        title = "제목", contentJson = """{"company":"카카오"}""",
        expiresAt = LocalDateTime.now().plusDays(14),
    )

    @Test
    fun `단건 조회 시 드래프트를 반환한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft

        val result = useCase.getDraft(GetBlockDraftQuery(draftId = 1L, userId = 10L))

        assertEquals(1L, result.id)
        assertEquals("제목", result.title)
    }

    @Test
    fun `존재하지 않는 드래프트 조회 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        every { blockDraftRepository.findById(999L) } returns null

        val ex = assertFailsWith<BusinessException> {
            useCase.getDraft(GetBlockDraftQuery(draftId = 999L, userId = 10L))
        }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `만료된 드래프트 조회 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        val expiredDraft = existingDraft.copy(expiresAt = LocalDateTime.now().minusDays(1))
        every { blockDraftRepository.findById(1L) } returns expiredDraft

        val ex = assertFailsWith<BusinessException> {
            useCase.getDraft(GetBlockDraftQuery(draftId = 1L, userId = 10L))
        }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 드래프트 조회 시 BLOCK_DRAFT_FORBIDDEN 예외가 발생한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft

        val ex = assertFailsWith<BusinessException> {
            useCase.getDraft(GetBlockDraftQuery(draftId = 1L, userId = 99L))
        }
        assertEquals(ErrorCode.BLOCK_DRAFT_FORBIDDEN, ex.errorCode)
    }
}
```

- [ ] **Step 4: GetBlockDraftUseCase 구현**

`module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftUseCase.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class GetBlockDraftQuery(
    val draftId: Long,
    val userId: Long,
)

class GetBlockDraftUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun getDraft(query: GetBlockDraftQuery): BlockDraft {
        val draft = blockDraftRepository.findById(query.draftId)
            ?: throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        if (draft.isExpired()) throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        if (!draft.isOwnedBy(query.userId)) throw BusinessException(ErrorCode.BLOCK_DRAFT_FORBIDDEN)
        return draft
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.GetBlockDraftsUseCaseTest" \
  --tests "com.atomiccv.resume.application.usecase.GetBlockDraftUseCaseTest"
```

Expected: PASS (7개)

- [ ] **Step 6: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftsUseCase.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftUseCase.kt \
  module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftsUseCaseTest.kt \
  module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlockDraftUseCaseTest.kt
git commit -m "feat(resume): GetBlockDraftsUseCase·GetBlockDraftUseCase 구현"
```

---

## Task 5: Delete UseCases

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftUseCase.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftsUseCase.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteAllBlockDraftsByTypeUseCase.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftUseCaseTest.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftsUseCaseTest.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteAllBlockDraftsByTypeUseCaseTest.kt`

- [ ] **Step 1: DeleteBlockDraftUseCase 테스트 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftUseCaseTest.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeleteBlockDraftUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = DeleteBlockDraftUseCase(blockDraftRepository)

    private val existingDraft = BlockDraft(
        id = 1L, userId = 10L, blockType = BlockType.CAREER,
        title = "제목", contentJson = "{}",
        expiresAt = LocalDateTime.now().plusDays(14),
    )

    @Test
    fun `단건 삭제 시 deleteById가 호출된다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft
        every { blockDraftRepository.deleteById(1L) } just runs

        useCase.delete(DeleteBlockDraftCommand(draftId = 1L, userId = 10L))

        verify { blockDraftRepository.deleteById(1L) }
    }

    @Test
    fun `존재하지 않는 드래프트 삭제 시 BLOCK_DRAFT_NOT_FOUND 예외가 발생한다`() {
        every { blockDraftRepository.findById(999L) } returns null

        val ex = assertFailsWith<BusinessException> {
            useCase.delete(DeleteBlockDraftCommand(draftId = 999L, userId = 10L))
        }
        assertEquals(ErrorCode.BLOCK_DRAFT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 드래프트 삭제 시 BLOCK_DRAFT_FORBIDDEN 예외가 발생한다`() {
        every { blockDraftRepository.findById(1L) } returns existingDraft

        val ex = assertFailsWith<BusinessException> {
            useCase.delete(DeleteBlockDraftCommand(draftId = 1L, userId = 99L))
        }
        assertEquals(ErrorCode.BLOCK_DRAFT_FORBIDDEN, ex.errorCode)
    }
}
```

- [ ] **Step 2: DeleteBlockDraftUseCase 구현**

`module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftUseCase.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class DeleteBlockDraftCommand(
    val draftId: Long,
    val userId: Long,
)

@Transactional
class DeleteBlockDraftUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun delete(command: DeleteBlockDraftCommand) {
        val draft = blockDraftRepository.findById(command.draftId)
            ?: throw BusinessException(ErrorCode.BLOCK_DRAFT_NOT_FOUND)
        if (!draft.isOwnedBy(command.userId)) throw BusinessException(ErrorCode.BLOCK_DRAFT_FORBIDDEN)
        blockDraftRepository.deleteById(command.draftId)
    }
}
```

- [ ] **Step 3: DeleteBlockDraftsUseCase 테스트 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftsUseCaseTest.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeleteBlockDraftsUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = DeleteBlockDraftsUseCase(blockDraftRepository)

    private fun draft(id: Long, userId: Long = 10L) = BlockDraft(
        id = id, userId = userId, blockType = BlockType.CAREER,
        title = "제목 $id", contentJson = "{}",
        expiresAt = LocalDateTime.now().plusDays(14),
    )

    @Test
    fun `선택 삭제 시 deleteAllByIds가 호출된다`() {
        every { blockDraftRepository.findAllByIds(listOf(1L, 2L)) } returns listOf(draft(1L), draft(2L))
        every { blockDraftRepository.deleteAllByIds(any()) } just runs

        useCase.deleteAll(DeleteBlockDraftsCommand(draftIds = listOf(1L, 2L), userId = 10L))

        verify { blockDraftRepository.deleteAllByIds(listOf(1L, 2L)) }
    }

    @Test
    fun `ids가 비어 있으면 아무 동작도 하지 않는다`() {
        useCase.deleteAll(DeleteBlockDraftsCommand(draftIds = emptyList(), userId = 10L))

        verify(exactly = 0) { blockDraftRepository.findAllByIds(any()) }
    }

    @Test
    fun `타인의 드래프트가 포함되면 BLOCK_DRAFT_FORBIDDEN 예외가 발생한다`() {
        every { blockDraftRepository.findAllByIds(listOf(1L, 2L)) } returns listOf(draft(1L), draft(2L, userId = 99L))

        val ex = assertFailsWith<BusinessException> {
            useCase.deleteAll(DeleteBlockDraftsCommand(draftIds = listOf(1L, 2L), userId = 10L))
        }
        assertEquals(ErrorCode.BLOCK_DRAFT_FORBIDDEN, ex.errorCode)
    }
}
```

- [ ] **Step 4: DeleteBlockDraftsUseCase 구현**

`module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftsUseCase.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class DeleteBlockDraftsCommand(
    val draftIds: List<Long>,
    val userId: Long,
)

@Transactional
class DeleteBlockDraftsUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun deleteAll(command: DeleteBlockDraftsCommand) {
        if (command.draftIds.isEmpty()) return
        val drafts = blockDraftRepository.findAllByIds(command.draftIds)
        if (drafts.any { !it.isOwnedBy(command.userId) }) {
            throw BusinessException(ErrorCode.BLOCK_DRAFT_FORBIDDEN)
        }
        blockDraftRepository.deleteAllByIds(command.draftIds)
    }
}
```

- [ ] **Step 5: DeleteAllBlockDraftsByTypeUseCase 테스트 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteAllBlockDraftsByTypeUseCaseTest.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test

class DeleteAllBlockDraftsByTypeUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = DeleteAllBlockDraftsByTypeUseCase(blockDraftRepository)

    @Test
    fun `타입별 전체 삭제 시 deleteAllByUserIdAndBlockType이 호출된다`() {
        every { blockDraftRepository.deleteAllByUserIdAndBlockType(10L, BlockType.CAREER) } just runs

        useCase.deleteAll(DeleteAllBlockDraftsByTypeCommand(userId = 10L, blockType = BlockType.CAREER))

        verify { blockDraftRepository.deleteAllByUserIdAndBlockType(10L, BlockType.CAREER) }
    }
}
```

- [ ] **Step 6: DeleteAllBlockDraftsByTypeUseCase 구현**

`module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteAllBlockDraftsByTypeUseCase.kt`:

```kotlin
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import org.springframework.transaction.annotation.Transactional

data class DeleteAllBlockDraftsByTypeCommand(
    val userId: Long,
    val blockType: BlockType,
)

@Transactional
class DeleteAllBlockDraftsByTypeUseCase(
    private val blockDraftRepository: BlockDraftRepository,
) {
    fun deleteAll(command: DeleteAllBlockDraftsByTypeCommand) {
        blockDraftRepository.deleteAllByUserIdAndBlockType(command.userId, command.blockType)
    }
}
```

- [ ] **Step 7: 전체 Delete UseCase 테스트 통과 확인**

```bash
./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.DeleteBlockDraftUseCaseTest" \
  --tests "com.atomiccv.resume.application.usecase.DeleteBlockDraftsUseCaseTest" \
  --tests "com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeUseCaseTest"
```

Expected: PASS (7개)

- [ ] **Step 8: 커밋**

```bash
git add \
  module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftUseCase.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftsUseCase.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteAllBlockDraftsByTypeUseCase.kt \
  module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftUseCaseTest.kt \
  module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockDraftsUseCaseTest.kt \
  module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteAllBlockDraftsByTypeUseCaseTest.kt
git commit -m "feat(resume): Delete BlockDraft UseCases 구현"
```

---

## Task 6: 스케줄러 & Bean 등록

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/scheduler/BlockDraftExpireScheduler.kt`
- Modify: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt`

- [ ] **Step 1: BlockDraftExpireScheduler 작성**

`module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/scheduler/BlockDraftExpireScheduler.kt`:

```kotlin
package com.atomiccv.resume.infrastructure.scheduler

import com.atomiccv.resume.domain.repository.BlockDraftRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BlockDraftExpireScheduler(
    private val blockDraftRepository: BlockDraftRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun deleteExpiredDrafts() {
        val deleted = blockDraftRepository.deleteAllExpiredBefore(LocalDateTime.now())
        log.info("만료 임시저장 삭제 완료: {}건", deleted)
    }
}
```

> `@EnableScheduling`은 `AtomicCvApplication`에 이미 선언되어 있으므로 추가 불필요.

- [ ] **Step 2: ResumeModuleConfiguration에 7개 UseCase Bean 등록**

`module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt`의 `ResumeModuleConfiguration` 클래스에 아래를 추가한다:

```kotlin
// 기존 import 목록에 추가
import com.atomiccv.resume.application.usecase.CreateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeUseCase
import com.atomiccv.resume.domain.repository.BlockDraftRepository

// ResumeModuleConfiguration 클래스 내부에 추가
@Bean
fun createBlockDraftUseCase(blockDraftRepository: BlockDraftRepository): CreateBlockDraftUseCase =
    CreateBlockDraftUseCase(blockDraftRepository)

@Bean
fun updateBlockDraftUseCase(blockDraftRepository: BlockDraftRepository): UpdateBlockDraftUseCase =
    UpdateBlockDraftUseCase(blockDraftRepository)

@Bean
fun getBlockDraftsUseCase(blockDraftRepository: BlockDraftRepository): GetBlockDraftsUseCase =
    GetBlockDraftsUseCase(blockDraftRepository)

@Bean
fun getBlockDraftUseCase(blockDraftRepository: BlockDraftRepository): GetBlockDraftUseCase =
    GetBlockDraftUseCase(blockDraftRepository)

@Bean
fun deleteBlockDraftUseCase(blockDraftRepository: BlockDraftRepository): DeleteBlockDraftUseCase =
    DeleteBlockDraftUseCase(blockDraftRepository)

@Bean
fun deleteBlockDraftsUseCase(blockDraftRepository: BlockDraftRepository): DeleteBlockDraftsUseCase =
    DeleteBlockDraftsUseCase(blockDraftRepository)

@Bean
fun deleteAllBlockDraftsByTypeUseCase(blockDraftRepository: BlockDraftRepository): DeleteAllBlockDraftsByTypeUseCase =
    DeleteAllBlockDraftsByTypeUseCase(blockDraftRepository)
```

- [ ] **Step 3: 빌드 확인**

```bash
./gradlew :module-resume:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add \
  module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/scheduler/BlockDraftExpireScheduler.kt \
  module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt
git commit -m "feat(resume): BlockDraftExpireScheduler 및 UseCase Bean 등록"
```

---

## Task 7: REST 컨트롤러

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockDraftController.kt`
- Test: `module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockDraftControllerTest.kt`

- [ ] **Step 1: BlockDraftControllerTest 작성**

`module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockDraftControllerTest.kt`:

```kotlin
package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.BlockDraftSummary
import com.atomiccv.resume.application.usecase.CreateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockDraftUseCase
import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime

@WebMvcTest(BlockDraftController::class)
@Import(BlockDraftControllerTest.MockConfig::class)
class BlockDraftControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var createBlockDraftUseCase: CreateBlockDraftUseCase
    @Autowired lateinit var updateBlockDraftUseCase: UpdateBlockDraftUseCase
    @Autowired lateinit var getBlockDraftsUseCase: GetBlockDraftsUseCase
    @Autowired lateinit var getBlockDraftUseCase: GetBlockDraftUseCase
    @Autowired lateinit var deleteBlockDraftUseCase: DeleteBlockDraftUseCase
    @Autowired lateinit var deleteBlockDraftsUseCase: DeleteBlockDraftsUseCase
    @Autowired lateinit var deleteAllBlockDraftsByTypeUseCase: DeleteAllBlockDraftsByTypeUseCase

    @TestConfiguration
    class MockConfig {
        @Bean fun createBlockDraftUseCase(): CreateBlockDraftUseCase = mockk()
        @Bean fun updateBlockDraftUseCase(): UpdateBlockDraftUseCase = mockk()
        @Bean fun getBlockDraftsUseCase(): GetBlockDraftsUseCase = mockk()
        @Bean fun getBlockDraftUseCase(): GetBlockDraftUseCase = mockk()
        @Bean fun deleteBlockDraftUseCase(): DeleteBlockDraftUseCase = mockk()
        @Bean fun deleteBlockDraftsUseCase(): DeleteBlockDraftsUseCase = mockk()
        @Bean fun deleteAllBlockDraftsByTypeUseCase(): DeleteAllBlockDraftsByTypeUseCase = mockk()
    }

    private val draft = BlockDraft(
        id = 1L, userId = 1L, blockType = BlockType.CAREER,
        title = "카카오 백엔드 개발자", contentJson = """{"company":"카카오"}""",
        expiresAt = LocalDateTime.of(2026, 6, 5, 14, 30),
        createdAt = LocalDateTime.of(2026, 5, 22, 14, 30),
        updatedAt = LocalDateTime.of(2026, 5, 22, 14, 30),
    )

    @Test
    @WithMockUser(username = "1")
    fun `POST api-block-drafts - 임시저장을 생성하고 반환한다`() {
        every { createBlockDraftUseCase.create(any()) } returns draft

        mockMvc.post("/api/block-drafts") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "blockType" to "CAREER",
                    "title" to "카카오 백엔드 개발자",
                    "contentJson" to mapOf("company" to "카카오"),
                ),
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.id") { value(1) }
            jsonPath("$.data.blockType") { value("CAREER") }
            jsonPath("$.data.title") { value("카카오 백엔드 개발자") }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-block-drafts - 블록 타입별 목록을 반환한다`() {
        val summary = BlockDraftSummary(
            id = 1L, title = "카카오 백엔드 개발자",
            updatedAt = LocalDateTime.of(2026, 5, 22, 14, 30),
            isActive = true,
        )
        every { getBlockDraftsUseCase.getDrafts(any()) } returns listOf(summary)

        mockMvc.get("/api/block-drafts?type=CAREER&currentDraftId=1").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data[0].id") { value(1) }
            jsonPath("$.data[0].isActive") { value(true) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-block-drafts-id - 단건 드래프트를 반환한다`() {
        every { getBlockDraftUseCase.getDraft(any()) } returns draft

        mockMvc.get("/api/block-drafts/1").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.id") { value(1) }
            jsonPath("$.data.title") { value("카카오 백엔드 개발자") }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-block-drafts-id - 드래프트를 덮어쓰고 반환한다`() {
        every { updateBlockDraftUseCase.update(any()) } returns draft

        mockMvc.put("/api/block-drafts/1") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("title" to "수정 제목", "contentJson" to emptyMap<String, Any>()),
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.id") { value(1) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-block-drafts-id - 단건 삭제 후 success를 반환한다`() {
        every { deleteBlockDraftUseCase.delete(any()) } just runs

        mockMvc.delete("/api/block-drafts/1") { with(csrf()) }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-block-drafts-batch - 선택 삭제 후 success를 반환한다`() {
        every { deleteBlockDraftsUseCase.deleteAll(any()) } just runs

        mockMvc.delete("/api/block-drafts/batch") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("ids" to listOf(1L, 2L)))
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-block-drafts with type - 타입별 전체 삭제 후 success를 반환한다`() {
        every { deleteAllBlockDraftsByTypeUseCase.deleteAll(any()) } just runs

        mockMvc.delete("/api/block-drafts?type=CAREER") { with(csrf()) }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :module-resume:test --tests "com.atomiccv.resume.interfaces.rest.BlockDraftControllerTest"
```

Expected: FAIL (BlockDraftController 클래스 없음)

- [ ] **Step 3: BlockDraftController 구현**

`module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockDraftController.kt`:

```kotlin
package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.BlockDraftSummary
import com.atomiccv.resume.application.usecase.CreateBlockDraftCommand
import com.atomiccv.resume.application.usecase.CreateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeCommand
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftCommand
import com.atomiccv.resume.application.usecase.DeleteBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsCommand
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftQuery
import com.atomiccv.resume.application.usecase.GetBlockDraftUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftsQuery
import com.atomiccv.resume.application.usecase.GetBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockDraftCommand
import com.atomiccv.resume.application.usecase.UpdateBlockDraftUseCase
import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/block-drafts")
class BlockDraftController(
    private val createBlockDraftUseCase: CreateBlockDraftUseCase,
    private val updateBlockDraftUseCase: UpdateBlockDraftUseCase,
    private val getBlockDraftsUseCase: GetBlockDraftsUseCase,
    private val getBlockDraftUseCase: GetBlockDraftUseCase,
    private val deleteBlockDraftUseCase: DeleteBlockDraftUseCase,
    private val deleteBlockDraftsUseCase: DeleteBlockDraftsUseCase,
    private val deleteAllBlockDraftsByTypeUseCase: DeleteAllBlockDraftsByTypeUseCase,
) {
    @PostMapping
    fun createDraft(
        authentication: Authentication,
        @Valid @RequestBody request: CreateBlockDraftRequest,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft = createBlockDraftUseCase.create(
            CreateBlockDraftCommand(
                userId = userId,
                blockType = request.blockType,
                title = request.title,
                contentJson = request.contentJson.toString(),
            ),
        )
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @GetMapping
    fun getDrafts(
        authentication: Authentication,
        @RequestParam type: BlockType,
        @RequestParam(required = false) currentDraftId: Long?,
    ): ResponseEntity<ApiResponse<List<BlockDraftSummaryResponse>>> {
        val userId = resolveUserId(authentication)
        val summaries = getBlockDraftsUseCase.getDrafts(
            GetBlockDraftsQuery(userId = userId, blockType = type, currentDraftId = currentDraftId),
        )
        return ResponseEntity.ok(ApiResponse.ok(summaries.map { it.toResponse() }))
    }

    @GetMapping("/{id}")
    fun getDraft(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft = getBlockDraftUseCase.getDraft(GetBlockDraftQuery(draftId = id, userId = userId))
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @PutMapping("/{id}")
    fun updateDraft(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBlockDraftRequest,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft = updateBlockDraftUseCase.update(
            UpdateBlockDraftCommand(
                draftId = id,
                userId = userId,
                title = request.title,
                contentJson = request.contentJson.toString(),
            ),
        )
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @DeleteMapping("/{id}")
    fun deleteDraft(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockDraftUseCase.delete(DeleteBlockDraftCommand(draftId = id, userId = userId))
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @DeleteMapping("/batch")
    fun deleteDrafts(
        authentication: Authentication,
        @Valid @RequestBody request: DeleteBlockDraftsRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockDraftsUseCase.deleteAll(
            DeleteBlockDraftsCommand(draftIds = request.ids, userId = userId),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @DeleteMapping
    fun deleteAllByType(
        authentication: Authentication,
        @RequestParam type: BlockType,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteAllBlockDraftsByTypeUseCase.deleteAll(
            DeleteAllBlockDraftsByTypeCommand(userId = userId, blockType = type),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
}

data class CreateBlockDraftRequest(
    val blockType: BlockType,
    val title: String,
    val contentJson: JsonNode,
)

data class UpdateBlockDraftRequest(
    val title: String,
    val contentJson: JsonNode,
)

data class DeleteBlockDraftsRequest(
    @field:Size(min = 1)
    val ids: List<Long>,
)

data class BlockDraftResponse(
    val id: Long,
    val blockType: BlockType,
    val title: String,
    val contentJson: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class BlockDraftSummaryResponse(
    val id: Long,
    val title: String,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
)

fun BlockDraft.toResponse() =
    BlockDraftResponse(
        id = id,
        blockType = blockType,
        title = title,
        contentJson = contentJson,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BlockDraftSummary.toResponse() =
    BlockDraftSummaryResponse(
        id = id,
        title = title,
        updatedAt = updatedAt,
        isActive = isActive,
    )
```

- [ ] **Step 4: 컨트롤러 테스트 통과 확인**

```bash
./gradlew :module-resume:test --tests "com.atomiccv.resume.interfaces.rest.BlockDraftControllerTest"
```

Expected: PASS (7개)

- [ ] **Step 5: module-resume 전체 테스트 통과 확인**

```bash
./gradlew :module-resume:test
```

Expected: BUILD SUCCESSFUL, 0 failures

- [ ] **Step 6: 커밋**

```bash
git add \
  module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockDraftController.kt \
  module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockDraftControllerTest.kt
git commit -m "feat(resume): BlockDraftController 구현"
```

---

## Task 8: 스펙 문서 & 계획 커밋

- [ ] **Step 1: 스펙 + 계획 문서 스테이징 및 커밋**

```bash
git add \
  docs/superpowers/specs/2026-05-22-block-draft-design.md \
  docs/superpowers/plans/2026-05-22-block-draft.md
git commit -m "docs: 블록 임시저장 스펙 및 구현 계획 문서 추가"
```
