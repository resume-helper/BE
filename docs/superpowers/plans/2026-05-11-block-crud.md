# Block CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `module-resume`에 블록 CRUD API 4개를 구현한다 (`GET/POST/PUT/DELETE /api/blocks`).

**Architecture:** Hexagonal. Domain(Block, BlockRepository) → Application(UseCase 4개) → Infrastructure(JPA) → Interfaces(REST). UseCase는 Spring 어노테이션 없이 작성하고 `ResumeModuleConfiguration`에서 `@Bean`으로 등록한다. auth 모듈의 `GlobalExceptionHandler`가 프로덕션에서 `BusinessException`을 처리한다.

**Tech Stack:** Kotlin, Spring Boot 3.5.14, Spring Data JPA, MySQL(JSON 컬럼), MockK 1.13.10 (UseCase 단위 테스트), `@WebMvcTest` + `@TestConfiguration` MockK (Controller 슬라이스 테스트)

---

## 파일 구조

| 파일 | 역할 |
|------|------|
| `module-resume/build.gradle.kts` | validation 의존성 추가 |
| `src/test/.../ResumeTestApplication.kt` | `@WebMvcTest` 부트스트랩용 테스트 앱 |
| `domain/model/Block.kt` | 블록 도메인 모델 (순수 Kotlin) |
| `domain/repository/BlockRepository.kt` | 저장소 인터페이스 |
| `application/usecase/CreateBlockUseCase.kt` | 생성 UseCase + CreateBlockCommand |
| `application/usecase/UpdateBlockUseCase.kt` | 수정 UseCase + UpdateBlockCommand |
| `application/usecase/DeleteBlockUseCase.kt` | 삭제(Soft Delete) UseCase |
| `application/usecase/GetBlocksUseCase.kt` | 목록 조회 UseCase + GetBlocksQuery |
| `infrastructure/persistence/BlockJpaEntity.kt` | JPA 엔티티 (BaseJpaEntity 상속) |
| `infrastructure/persistence/BlockJpaRepository.kt` | Spring Data JPA 리포지토리 |
| `infrastructure/persistence/BlockRepositoryImpl.kt` | BlockRepository 구현체 |
| `infrastructure/ResumeModuleConfiguration.kt` | UseCase @Bean 등록 |
| `interfaces/rest/BlockController.kt` | REST 컨트롤러 + Request/Response DTO |

테스트 파일:

| 파일 | 방식 |
|------|------|
| `test/.../CreateBlockUseCaseTest.kt` | MockK 단위 테스트 |
| `test/.../UpdateBlockUseCaseTest.kt` | MockK 단위 테스트 |
| `test/.../DeleteBlockUseCaseTest.kt` | MockK 단위 테스트 |
| `test/.../GetBlocksUseCaseTest.kt` | MockK 단위 테스트 |
| `test/.../BlockControllerTest.kt` | `@WebMvcTest` + `@TestConfiguration` MockK |

---

### Task 1: 의존성 + 테스트 앱 세팅

**Files:**
- Modify: `module-resume/build.gradle.kts`
- Create: `module-resume/src/test/kotlin/com/atomiccv/resume/ResumeTestApplication.kt`

- [ ] **Step 1: build.gradle.kts 수정**

```kotlin
// module-resume/build.gradle.kts
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.14"))
    implementation(project(":module-shared"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.mysql:mysql-connector-j")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

- [ ] **Step 2: ResumeTestApplication.kt 작성**

```kotlin
// module-resume/src/test/kotlin/com/atomiccv/resume/ResumeTestApplication.kt
package com.atomiccv.resume

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.test.context.ActiveProfiles

@SpringBootApplication
@ActiveProfiles("test")
class ResumeTestApplication
```

- [ ] **Step 3: 빌드 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:compileKotlin :module-resume:compileTestKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add module-resume/build.gradle.kts \
        module-resume/src/test/kotlin/com/atomiccv/resume/ResumeTestApplication.kt
git commit -m "chore(resume): validation 의존성·테스트 앱 추가"
```

---

### Task 2: Block 도메인 모델 + BlockRepository 인터페이스

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/domain/model/Block.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/domain/repository/BlockRepository.kt`

- [ ] **Step 1: Block.kt 작성**

도메인 레이어는 Spring·JPA import 금지. 순수 Kotlin data class.

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/domain/model/Block.kt
package com.atomiccv.resume.domain.model

import java.time.LocalDateTime

data class Block(
    val id: Long = 0,
    val userId: Long,
    val type: BlockType,
    val title: String,
    val contentJson: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
) {
    fun isDeleted(): Boolean = deletedAt != null

    fun isOwnedBy(ownerId: Long): Boolean = userId == ownerId
}
```

- [ ] **Step 2: BlockRepository.kt 작성**

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/domain/repository/BlockRepository.kt
package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType

interface BlockRepository {
    fun save(block: Block): Block

    fun findById(id: Long): Block?

    fun findAllActiveByUserId(userId: Long): List<Block>

    fun findAllActiveByUserIdAndType(userId: Long, type: BlockType): List<Block>
}
```

- [ ] **Step 3: 빌드 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:compileKotlin
```

- [ ] **Step 4: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/domain/
git commit -m "feat(resume): Block 도메인 모델·BlockRepository 인터페이스 추가"
```

---

### Task 3: CreateBlockUseCase (TDD)

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/CreateBlockUseCase.kt`
- Create: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/CreateBlockUseCaseTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/CreateBlockUseCaseTest.kt
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CreateBlockUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = CreateBlockUseCase(blockRepository)

    @Test
    fun `블록 생성 시 userId·type·title·contentJson이 저장되고 반환된다`() {
        val command = CreateBlockCommand(
            userId = 1L,
            type = BlockType.CAREER,
            title = "카카오 백엔드 개발자",
            contentJson = """{"company":"카카오"}""",
        )
        val saved = Block(id = 10L, userId = 1L, type = BlockType.CAREER, title = "카카오 백엔드 개발자", contentJson = """{"company":"카카오"}""")
        every { blockRepository.save(any()) } returns saved

        val result = useCase.create(command)

        assertEquals(10L, result.id)
        assertEquals(BlockType.CAREER, result.type)
        verify {
            blockRepository.save(match {
                it.userId == 1L && it.type == BlockType.CAREER && it.title == "카카오 백엔드 개발자"
            })
        }
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.CreateBlockUseCaseTest" --continue 2>&1 | tail -15
```
Expected: FAILED (CreateBlockUseCase not found)

- [ ] **Step 3: CreateBlockUseCase 구현**

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/CreateBlockUseCase.kt
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.transaction.annotation.Transactional

data class CreateBlockCommand(
    val userId: Long,
    val type: BlockType,
    val title: String,
    val contentJson: String,
)

@Transactional
class CreateBlockUseCase(
    private val blockRepository: BlockRepository,
) {
    fun create(command: CreateBlockCommand): Block =
        blockRepository.save(
            Block(
                userId = command.userId,
                type = command.type,
                title = command.title,
                contentJson = command.contentJson,
            ),
        )
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.CreateBlockUseCaseTest"
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/CreateBlockUseCase.kt \
        module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/CreateBlockUseCaseTest.kt
git commit -m "feat(resume): CreateBlockUseCase 구현"
```

---

### Task 4: UpdateBlockUseCase (TDD)

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockUseCase.kt`
- Create: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockUseCaseTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockUseCaseTest.kt
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateBlockUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = UpdateBlockUseCase(blockRepository)

    private val existingBlock = Block(
        id = 1L,
        userId = 10L,
        type = BlockType.CAREER,
        title = "구 제목",
        contentJson = "{}",
    )

    @Test
    fun `블록 수정 시 title과 contentJson이 업데이트된 블록이 저장된다`() {
        val command = UpdateBlockCommand(blockId = 1L, userId = 10L, title = "신 제목", contentJson = """{"new":true}""")
        val updated = existingBlock.copy(title = "신 제목", contentJson = """{"new":true}""")
        every { blockRepository.findById(1L) } returns existingBlock
        every { blockRepository.save(any()) } returns updated

        val result = useCase.update(command)

        assertEquals("신 제목", result.title)
        verify { blockRepository.save(match { it.title == "신 제목" && it.contentJson == """{"new":true}""" }) }
    }

    @Test
    fun `존재하지 않는 블록 수정 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { blockRepository.findById(999L) } returns null

        val ex = assertFailsWith<BusinessException> {
            useCase.update(UpdateBlockCommand(blockId = 999L, userId = 10L, title = "제목", contentJson = "{}"))
        }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `삭제된 블록 수정 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { blockRepository.findById(1L) } returns existingBlock.copy(deletedAt = LocalDateTime.now())

        val ex = assertFailsWith<BusinessException> {
            useCase.update(UpdateBlockCommand(blockId = 1L, userId = 10L, title = "제목", contentJson = "{}"))
        }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 블록 수정 시 FORBIDDEN 예외가 발생한다`() {
        every { blockRepository.findById(1L) } returns existingBlock

        val ex = assertFailsWith<BusinessException> {
            useCase.update(UpdateBlockCommand(blockId = 1L, userId = 99L, title = "제목", contentJson = "{}"))
        }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.UpdateBlockUseCaseTest" --continue 2>&1 | tail -15
```

- [ ] **Step 3: UpdateBlockUseCase 구현**

Detekt 규칙 준수: `update` 함수 내 throw는 1개, `findActiveBlock` 내 throw는 2개.

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockUseCase.kt
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class UpdateBlockCommand(
    val blockId: Long,
    val userId: Long,
    val title: String,
    val contentJson: String,
)

@Transactional
class UpdateBlockUseCase(
    private val blockRepository: BlockRepository,
) {
    fun update(command: UpdateBlockCommand): Block {
        val block = findActiveBlock(command.blockId)
        if (!block.isOwnedBy(command.userId)) throw BusinessException(ErrorCode.FORBIDDEN)
        return blockRepository.save(block.copy(title = command.title, contentJson = command.contentJson))
    }

    private fun findActiveBlock(blockId: Long): Block {
        val block = blockRepository.findById(blockId)
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "블록을 찾을 수 없습니다.")
        if (block.isDeleted()) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "블록을 찾을 수 없습니다.")
        return block
    }
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.UpdateBlockUseCaseTest"
```

- [ ] **Step 5: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockUseCase.kt \
        module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/UpdateBlockUseCaseTest.kt
git commit -m "feat(resume): UpdateBlockUseCase 구현"
```

---

### Task 5: DeleteBlockUseCase (TDD)

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockUseCase.kt`
- Create: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockUseCaseTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockUseCaseTest.kt
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeleteBlockUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = DeleteBlockUseCase(blockRepository)

    private val existingBlock = Block(id = 1L, userId = 10L, type = BlockType.SKILL, title = "Kotlin", contentJson = "{}")

    @Test
    fun `블록 삭제 시 deletedAt이 설정된 블록이 저장된다`() {
        every { blockRepository.findById(1L) } returns existingBlock
        every { blockRepository.save(any()) } returns existingBlock.copy(deletedAt = LocalDateTime.now())

        useCase.delete(blockId = 1L, userId = 10L)

        verify { blockRepository.save(match { it.deletedAt != null }) }
    }

    @Test
    fun `존재하지 않는 블록 삭제 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { blockRepository.findById(999L) } returns null

        val ex = assertFailsWith<BusinessException> { useCase.delete(blockId = 999L, userId = 10L) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `이미 삭제된 블록 삭제 시 RESOURCE_NOT_FOUND 예외가 발생한다`() {
        every { blockRepository.findById(1L) } returns existingBlock.copy(deletedAt = LocalDateTime.now())

        val ex = assertFailsWith<BusinessException> { useCase.delete(blockId = 1L, userId = 10L) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `타인의 블록 삭제 시 FORBIDDEN 예외가 발생한다`() {
        every { blockRepository.findById(1L) } returns existingBlock

        val ex = assertFailsWith<BusinessException> { useCase.delete(blockId = 1L, userId = 99L) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.DeleteBlockUseCaseTest" --continue 2>&1 | tail -15
```

- [ ] **Step 3: DeleteBlockUseCase 구현**

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockUseCase.kt
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.repository.BlockRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
class DeleteBlockUseCase(
    private val blockRepository: BlockRepository,
) {
    fun delete(blockId: Long, userId: Long) {
        val block = findActiveBlock(blockId)
        if (!block.isOwnedBy(userId)) throw BusinessException(ErrorCode.FORBIDDEN)
        blockRepository.save(block.copy(deletedAt = LocalDateTime.now()))
    }

    private fun findActiveBlock(blockId: Long): Block {
        val block = blockRepository.findById(blockId)
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "블록을 찾을 수 없습니다.")
        if (block.isDeleted()) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "블록을 찾을 수 없습니다.")
        return block
    }
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.DeleteBlockUseCaseTest"
```

- [ ] **Step 5: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockUseCase.kt \
        module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/DeleteBlockUseCaseTest.kt
git commit -m "feat(resume): DeleteBlockUseCase 구현"
```

---

### Task 6: GetBlocksUseCase (TDD)

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlocksUseCase.kt`
- Create: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlocksUseCaseTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlocksUseCaseTest.kt
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetBlocksUseCaseTest {
    private val blockRepository: BlockRepository = mockk()
    private val useCase = GetBlocksUseCase(blockRepository)

    private val careerBlock = Block(id = 1L, userId = 10L, type = BlockType.CAREER, title = "경력1", contentJson = "{}")
    private val skillBlock = Block(id = 2L, userId = 10L, type = BlockType.SKILL, title = "기술1", contentJson = "{}")

    @Test
    fun `type 없이 조회하면 유저의 모든 활성 블록을 반환한다`() {
        every { blockRepository.findAllActiveByUserId(10L) } returns listOf(careerBlock, skillBlock)

        val result = useCase.getBlocks(GetBlocksQuery(userId = 10L, type = null))

        assertEquals(2, result.size)
        verify { blockRepository.findAllActiveByUserId(10L) }
    }

    @Test
    fun `type 필터를 주면 해당 type 블록만 반환한다`() {
        every { blockRepository.findAllActiveByUserIdAndType(10L, BlockType.CAREER) } returns listOf(careerBlock)

        val result = useCase.getBlocks(GetBlocksQuery(userId = 10L, type = BlockType.CAREER))

        assertEquals(1, result.size)
        assertEquals(BlockType.CAREER, result[0].type)
        verify { blockRepository.findAllActiveByUserIdAndType(10L, BlockType.CAREER) }
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.GetBlocksUseCaseTest" --continue 2>&1 | tail -15
```

- [ ] **Step 3: GetBlocksUseCase 구현**

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlocksUseCase.kt
package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository

data class GetBlocksQuery(
    val userId: Long,
    val type: BlockType? = null,
)

class GetBlocksUseCase(
    private val blockRepository: BlockRepository,
) {
    fun getBlocks(query: GetBlocksQuery): List<Block> =
        if (query.type != null) {
            blockRepository.findAllActiveByUserIdAndType(query.userId, query.type)
        } else {
            blockRepository.findAllActiveByUserId(query.userId)
        }
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.application.usecase.GetBlocksUseCaseTest"
```

- [ ] **Step 5: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetBlocksUseCase.kt \
        module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetBlocksUseCaseTest.kt
git commit -m "feat(resume): GetBlocksUseCase 구현"
```

---

### Task 7: JPA 인프라 (BlockJpaEntity + BlockJpaRepository + BlockRepositoryImpl)

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockJpaEntity.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockJpaRepository.kt`
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockRepositoryImpl.kt`

참고: `BaseJpaEntity`는 `module-shared`의 `com.atomiccv.shared.infrastructure.persistence.BaseJpaEntity`. `createdAt`/`updatedAt`은 JPA Auditing이 자동 관리.

- [ ] **Step 1: BlockJpaEntity.kt 작성**

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockJpaEntity.kt
package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Block
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
@Table(name = "blocks")
class BlockJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: BlockType,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(name = "content_json", nullable = false, columnDefinition = "JSON")
    val contentJson: String,
    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,
) : BaseJpaEntity() {
    fun toDomain() = Block(
        id = id,
        userId = userId,
        type = type,
        title = title,
        contentJson = contentJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    companion object {
        fun fromDomain(block: Block) = BlockJpaEntity(
            id = block.id,
            userId = block.userId,
            type = block.type,
            title = block.title,
            contentJson = block.contentJson,
            deletedAt = block.deletedAt,
        )
    }
}
```

- [ ] **Step 2: BlockJpaRepository.kt 작성**

Spring Data JPA 쿼리 메서드 이름이 `deleted_at IS NULL` 조건을 포함한다.

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockJpaRepository.kt
package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import org.springframework.data.jpa.repository.JpaRepository

interface BlockJpaRepository : JpaRepository<BlockJpaEntity, Long> {
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long): List<BlockJpaEntity>

    fun findAllByUserIdAndTypeAndDeletedAtIsNull(userId: Long, type: BlockType): List<BlockJpaEntity>
}
```

- [ ] **Step 3: BlockRepositoryImpl.kt 작성**

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/BlockRepositoryImpl.kt
package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.stereotype.Repository

@Repository
class BlockRepositoryImpl(
    private val jpaRepository: BlockJpaRepository,
) : BlockRepository {
    override fun save(block: Block): Block =
        jpaRepository.save(BlockJpaEntity.fromDomain(block)).toDomain()

    override fun findById(id: Long): Block? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllActiveByUserId(userId: Long): List<Block> =
        jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId).map { it.toDomain() }

    override fun findAllActiveByUserIdAndType(userId: Long, type: BlockType): List<Block> =
        jpaRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(userId, type).map { it.toDomain() }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/persistence/
git commit -m "feat(resume): Block JPA 인프라 구현 (Entity·JpaRepository·RepositoryImpl)"
```

---

### Task 8: ResumeModuleConfiguration

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt`

- [ ] **Step 1: ResumeModuleConfiguration.kt 작성**

auth 모듈의 `AuthConfiguration` 패턴과 동일: UseCase는 `@Component` 없이 작성하고 여기서 `@Bean`으로 등록.

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt
package com.atomiccv.resume.infrastructure

import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ResumeModuleConfiguration {
    @Bean
    fun createBlockUseCase(blockRepository: BlockRepository): CreateBlockUseCase =
        CreateBlockUseCase(blockRepository)

    @Bean
    fun updateBlockUseCase(blockRepository: BlockRepository): UpdateBlockUseCase =
        UpdateBlockUseCase(blockRepository)

    @Bean
    fun deleteBlockUseCase(blockRepository: BlockRepository): DeleteBlockUseCase =
        DeleteBlockUseCase(blockRepository)

    @Bean
    fun getBlocksUseCase(blockRepository: BlockRepository): GetBlocksUseCase =
        GetBlocksUseCase(blockRepository)
}
```

- [ ] **Step 2: 빌드 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:compileKotlin
```

- [ ] **Step 3: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt
git commit -m "feat(resume): ResumeModuleConfiguration UseCase 빈 등록"
```

---

### Task 9: BlockController (TDD)

**Files:**
- Create: `module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockController.kt`
- Create: `module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockControllerTest.kt`

컨트롤러 테스트 패턴: auth 모듈의 `AuthControllerTest`와 동일. `@TestConfiguration`으로 MockK 빈을 제공하고 `@WithMockUser(username = "1")`로 userId를 주입한다. `with(csrf())`는 POST/PUT/DELETE에 필수.

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockControllerTest.kt
package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.domain.model.Block
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

@WebMvcTest(BlockController::class)
@Import(BlockControllerTest.MockConfig::class)
class BlockControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var createBlockUseCase: CreateBlockUseCase

    @Autowired
    lateinit var updateBlockUseCase: UpdateBlockUseCase

    @Autowired
    lateinit var deleteBlockUseCase: DeleteBlockUseCase

    @Autowired
    lateinit var getBlocksUseCase: GetBlocksUseCase

    private val block = Block(
        id = 1L,
        userId = 1L,
        type = BlockType.CAREER,
        title = "카카오 백엔드 개발자",
        contentJson = """{"company":"카카오"}""",
        createdAt = LocalDateTime.of(2026, 5, 11, 10, 0),
        updatedAt = LocalDateTime.of(2026, 5, 11, 10, 0),
    )

    @TestConfiguration
    class MockConfig {
        @Bean
        fun createBlockUseCase(): CreateBlockUseCase = mockk()

        @Bean
        fun updateBlockUseCase(): UpdateBlockUseCase = mockk()

        @Bean
        fun deleteBlockUseCase(): DeleteBlockUseCase = mockk()

        @Bean
        fun getBlocksUseCase(): GetBlocksUseCase = mockk()
    }

    @Test
    @WithMockUser(username = "1")
    fun `GET api-blocks - 블록 목록을 반환한다`() {
        every { getBlocksUseCase.getBlocks(any()) } returns listOf(block)

        mockMvc.get("/api/blocks").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data[0].id") { value(1) }
            jsonPath("$.data[0].type") { value("CAREER") }
            jsonPath("$.data[0].title") { value("카카오 백엔드 개발자") }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `POST api-blocks - 블록을 생성하고 반환한다`() {
        every { createBlockUseCase.create(any()) } returns block

        mockMvc.post("/api/blocks") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("type" to "CAREER", "title" to "카카오 백엔드 개발자", "contentJson" to """{"company":"카카오"}"""),
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.id") { value(1) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `PUT api-blocks-id - 블록을 수정하고 반환한다`() {
        every { updateBlockUseCase.update(any()) } returns block

        mockMvc.put("/api/blocks/1") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("title" to "수정 제목", "contentJson" to "{}"),
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.id") { value(1) }
        }
    }

    @Test
    @WithMockUser(username = "1")
    fun `DELETE api-blocks-id - 블록을 삭제하고 success를 반환한다`() {
        every { deleteBlockUseCase.delete(1L, 1L) } just runs

        mockMvc.delete("/api/blocks/1") {
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.interfaces.rest.BlockControllerTest" --continue 2>&1 | tail -20
```
Expected: FAILED (BlockController not found)

- [ ] **Step 3: BlockController.kt 작성**

```kotlin
// module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockController.kt
package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.CreateBlockCommand
import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.GetBlocksQuery
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockCommand
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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
@RequestMapping("/api/blocks")
class BlockController(
    private val createBlockUseCase: CreateBlockUseCase,
    private val updateBlockUseCase: UpdateBlockUseCase,
    private val deleteBlockUseCase: DeleteBlockUseCase,
    private val getBlocksUseCase: GetBlocksUseCase,
) {
    @GetMapping
    fun getBlocks(
        authentication: Authentication,
        @RequestParam(required = false) type: BlockType?,
    ): ResponseEntity<ApiResponse<List<BlockResponse>>> {
        val userId = resolveUserId(authentication)
        val blocks = getBlocksUseCase.getBlocks(GetBlocksQuery(userId = userId, type = type))
        return ResponseEntity.ok(ApiResponse.ok(blocks.map { it.toResponse() }))
    }

    @PostMapping
    fun createBlock(
        authentication: Authentication,
        @Valid @RequestBody request: CreateBlockRequest,
    ): ResponseEntity<ApiResponse<BlockResponse>> {
        val userId = resolveUserId(authentication)
        val block = createBlockUseCase.create(
            CreateBlockCommand(userId = userId, type = request.type, title = request.title, contentJson = request.contentJson),
        )
        return ResponseEntity.ok(ApiResponse.ok(block.toResponse()))
    }

    @PutMapping("/{id}")
    fun updateBlock(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBlockRequest,
    ): ResponseEntity<ApiResponse<BlockResponse>> {
        val userId = resolveUserId(authentication)
        val block = updateBlockUseCase.update(
            UpdateBlockCommand(blockId = id, userId = userId, title = request.title, contentJson = request.contentJson),
        )
        return ResponseEntity.ok(ApiResponse.ok(block.toResponse()))
    }

    @DeleteMapping("/{id}")
    fun deleteBlock(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockUseCase.delete(blockId = id, userId = userId)
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
}

data class CreateBlockRequest(
    val type: BlockType,
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:NotBlank
    val contentJson: String,
)

data class UpdateBlockRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:NotBlank
    val contentJson: String,
)

data class BlockResponse(
    val id: Long,
    val type: BlockType,
    val title: String,
    val contentJson: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun Block.toResponse() = BlockResponse(
    id = id,
    type = type,
    title = title,
    contentJson = contentJson,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test \
  --tests "com.atomiccv.resume.interfaces.rest.BlockControllerTest"
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/BlockController.kt \
        module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/BlockControllerTest.kt
git commit -m "feat(resume): BlockController 구현"
```

---

### Task 10: 전체 테스트 + Detekt + ktlint + TASKS.md 업데이트

- [ ] **Step 1: 전체 테스트 실행**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:test
```
Expected: BUILD SUCCESSFUL, 모든 테스트 통과

- [ ] **Step 2: Detekt 검사**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:detekt
```
Expected: BUILD SUCCESSFUL (위반 없음)

- [ ] **Step 3: ktlint 포맷 + 검사**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:ktlintFormat
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-resume:ktlintCheck
```
Expected: 두 명령 모두 BUILD SUCCESSFUL

- [ ] **Step 4: TASKS.md 상태 업데이트**

`doc/TASKS.md`의 2-1 블록 기능 항목 #1을 🟢로 변경한다:

```markdown
### 2-1. 블록 (Block) 기능

| # | 작업 | 담당 | 상태 |
|---|------|-----|------|
| 1 | 블록 생성 / 수정 / 삭제 | | 🟢 |
| 2 | 블록 순서 변경 | | 🔴 |
| 3 | block_versions 이력 저장 | | ⏸ 보류 |
| 4 | 블록 버전 복원 | |⏸ 보류 |
```

- [ ] **Step 5: 최종 커밋**

```bash
git add doc/TASKS.md doc/SERVICE_POLICY.md doc/prd.md \
        docs/superpowers/specs/2026-05-11-block-crud-design.md \
        docs/superpowers/plans/2026-05-11-block-crud.md
git commit -m "docs: 블록 기능 설계 스펙·플랜·TASKS 업데이트"
```
