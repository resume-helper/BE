# 이미지/파일 업로드 공동 모듈 추출 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** S3 presigned URL 업로드 기능을 `module-resume` 에서 `module-shared` 로 이동, 다른 도메인 모듈이 재사용 가능하게 한다.

**Architecture:** 위치 이동(리팩토링) 만 수행. 기존 엔드포인트(`POST /api/resumes/upload-url`), DTO, key prefix 그대로. 새 비즈니스 로직·새 엔드포인트 없음. AWS S3 SDK 의존성은 `module-shared` 로 이전.

**Tech Stack:** Kotlin, Spring Boot 3.5, Gradle (multi-module), AWS SDK v2 (`software.amazon.awssdk:s3`), JUnit 5, MockK.

**Spec:** `doc/specs/2026-06-13-shared-image-upload-design.md`

---

## File Structure

### 생성 (in `module-shared`)
- `src/main/kotlin/com/atomiccv/shared/application/port/S3Port.kt` — 업로드/다운로드 presigned URL 인터페이스
- `src/main/kotlin/com/atomiccv/shared/application/usecase/GenerateUploadUrlUseCase.kt` — UseCase + Command + Result (한 파일)
- `src/main/kotlin/com/atomiccv/shared/infrastructure/storage/S3Adapter.kt` — `S3Port` 의 AWS SDK 구현
- `src/main/kotlin/com/atomiccv/shared/infrastructure/storage/SharedStorageConfiguration.kt` — `S3Presigner` + `S3Adapter` + `GenerateUploadUrlUseCase` Bean 정의
- `src/test/kotlin/com/atomiccv/shared/application/usecase/GenerateUploadUrlUseCaseTest.kt` — 기존 테스트 이동

### 삭제 (in `module-resume`)
- `src/main/kotlin/com/atomiccv/resume/application/port/S3Port.kt`
- `src/main/kotlin/com/atomiccv/resume/application/usecase/GenerateUploadUrlUseCase.kt`
- `src/main/kotlin/com/atomiccv/resume/infrastructure/s3/S3Adapter.kt` (디렉토리 `s3/` 도 비어지면 제거)
- `src/test/kotlin/com/atomiccv/resume/application/usecase/GenerateUploadUrlUseCaseTest.kt`

### 수정
- `module-shared/build.gradle.kts` — AWS SDK 의존성 추가 + 테스트 의존성 추가
- `module-resume/build.gradle.kts` — AWS SDK 의존성 제거
- `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt` — S3 관련 Bean 제거, 미사용 import 제거
- `module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/ResumeController.kt` — `GenerateUploadUrlUseCase` import 경로 갱신
- `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetResumeUseCase.kt` — `S3Port` import 경로 갱신
- `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetResumeUseCaseTest.kt` — `S3Port` import 경로 갱신
- `module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/ResumeControllerTest.kt` — UseCase import 경로 갱신
- `doc/MODULE_STRUCTURE.md` — `:module-shared` 섹션에 storage / application.usecase 항목 추가

---

## Task 1: module-shared 에 S3 SDK + 테스트 의존성 추가

**Files:**
- Modify: `module-shared/build.gradle.kts`

- [ ] **Step 1: 의존성 추가**

`module-shared/build.gradle.kts` 의 `dependencies { ... }` 블록을 다음으로 교체:

```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.14"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation(platform("software.amazon.awssdk:bom:2.26.12"))
    implementation("software.amazon.awssdk:s3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

- [ ] **Step 2: 의존성 해석 확인**

Run: `./gradlew :module-shared:dependencies --configuration runtimeClasspath 2>&1 | grep -E "awssdk|mockk" | head -5`

Expected: `software.amazon.awssdk:s3:...` 출력. (테스트 의존성은 testRuntime 에서 확인)

---

## Task 2: S3Port 인터페이스 module-shared 로 생성

**Files:**
- Create: `module-shared/src/main/kotlin/com/atomiccv/shared/application/port/S3Port.kt`

- [ ] **Step 1: 파일 생성**

```kotlin
package com.atomiccv.shared.application.port

interface S3Port {
    fun generateUploadPresignedUrl(
        key: String,
        expiryMinutes: Long,
    ): String

    fun generateDownloadPresignedUrl(
        key: String,
        expiryMinutes: Long,
    ): String
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :module-shared:compileKotlin`
Expected: `BUILD SUCCESSFUL`

---

## Task 3: GenerateUploadUrlUseCase + Command + Result module-shared 로 생성

**Files:**
- Create: `module-shared/src/main/kotlin/com/atomiccv/shared/application/usecase/GenerateUploadUrlUseCase.kt`

- [ ] **Step 1: 파일 생성**

```kotlin
package com.atomiccv.shared.application.usecase

import com.atomiccv.shared.application.port.S3Port
import java.util.UUID

data class GenerateUploadUrlCommand(
    val userId: Long,
    val fileName: String,
)

data class UploadUrlResult(
    val presignedUrl: String,
    val s3Key: String,
)

class GenerateUploadUrlUseCase(
    private val s3Port: S3Port,
) {
    fun generate(command: GenerateUploadUrlCommand): UploadUrlResult {
        val key = buildKey(command)
        val url = s3Port.generateUploadPresignedUrl(key, UPLOAD_URL_EXPIRY_MINUTES)
        return UploadUrlResult(presignedUrl = url, s3Key = key)
    }

    private fun buildKey(command: GenerateUploadUrlCommand): String =
        "resumes/${command.userId}/${UUID.randomUUID()}/${command.fileName}"

    companion object {
        private const val UPLOAD_URL_EXPIRY_MINUTES = 10L
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :module-shared:compileKotlin`
Expected: `BUILD SUCCESSFUL`

---

## Task 4: S3Adapter module-shared 로 생성

**Files:**
- Create: `module-shared/src/main/kotlin/com/atomiccv/shared/infrastructure/storage/S3Adapter.kt`

- [ ] **Step 1: 파일 생성**

```kotlin
package com.atomiccv.shared.infrastructure.storage

import com.atomiccv.shared.application.port.S3Port
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

class S3Adapter(
    private val presigner: S3Presigner,
    private val bucketName: String,
) : S3Port {
    override fun generateUploadPresignedUrl(
        key: String,
        expiryMinutes: Long,
    ): String {
        val duration = Duration.ofMinutes(expiryMinutes)
        val builder =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(duration)
                .putObjectRequest { it.bucket(bucketName).key(key) }
        val request = builder.build()
        return presigner
            .presignPutObject(request)
            .url()
            .toString()
    }

    override fun generateDownloadPresignedUrl(
        key: String,
        expiryMinutes: Long,
    ): String {
        val duration = Duration.ofMinutes(expiryMinutes)
        val builder =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(duration)
                .getObjectRequest { it.bucket(bucketName).key(key) }
        val request = builder.build()
        return presigner
            .presignGetObject(request)
            .url()
            .toString()
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :module-shared:compileKotlin`
Expected: `BUILD SUCCESSFUL`

---

## Task 5: SharedStorageConfiguration 신규 생성 (Bean 정의)

**Files:**
- Create: `module-shared/src/main/kotlin/com/atomiccv/shared/infrastructure/storage/SharedStorageConfiguration.kt`

- [ ] **Step 1: 파일 생성**

```kotlin
package com.atomiccv.shared.infrastructure.storage

import com.atomiccv.shared.application.port.S3Port
import com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class SharedStorageConfiguration {
    @Bean
    fun s3Presigner(
        @Value("\${cloud.aws.region.static}") region: String,
    ): S3Presigner = S3Presigner.builder().region(Region.of(region)).build()

    @Bean
    fun s3Port(
        presigner: S3Presigner,
        @Value("\${resume.s3.bucket-name}") bucketName: String,
    ): S3Port = S3Adapter(presigner, bucketName)

    @Bean
    fun generateUploadUrlUseCase(s3Port: S3Port): GenerateUploadUrlUseCase = GenerateUploadUrlUseCase(s3Port)
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :module-shared:compileKotlin`
Expected: `BUILD SUCCESSFUL`

---

## Task 6: 기존 caller 의 import 경로 갱신

`module-resume` 의 코드 (Configuration, Controller, UseCase, Test) 가 새 위치(`com.atomiccv.shared.*`) 를 가리키도록 변경. 이 단계가 끝나면 module-resume 의 원본 파일들은 unused (다음 task 에서 삭제).

**Files:**
- Modify: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt`
- Modify: `module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/ResumeController.kt`
- Modify: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetResumeUseCase.kt`
- Modify: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetResumeUseCaseTest.kt`
- Modify: `module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/ResumeControllerTest.kt`

- [ ] **Step 1: ResumeModuleConfiguration 정리**

다음 변경을 적용:

1. import 갱신 — 3행 `com.atomiccv.resume.application.port.S3Port` → `com.atomiccv.shared.application.port.S3Port`
2. import 갱신 — 13행 `com.atomiccv.resume.application.usecase.GenerateUploadUrlUseCase` → `com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCase`
3. import 제거 — 33행 `com.atomiccv.resume.infrastructure.s3.S3Adapter` (더 이상 사용 안 함)
4. import 제거 — `software.amazon.awssdk.regions.Region`, `software.amazon.awssdk.services.s3.presigner.S3Presigner` (Bean 제거로 미사용)
5. `ResumeUseCaseConfiguration` 안의 다음 Bean 들 **삭제**:
   - `fun s3Presigner(...)` Bean
   - `fun s3Port(...)` Bean
   - `fun generateUploadUrlUseCase(...)` Bean

`getResumeUseCase` Bean 은 그대로 유지 (s3Port 파라미터는 Spring 이 SharedStorageConfiguration 의 Bean 으로 자동 주입).

- [ ] **Step 2: ResumeController import 갱신**

```diff
- import com.atomiccv.resume.application.usecase.GenerateUploadUrlUseCase
+ import com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCase
```

`GenerateUploadUrlCommand`, `UploadUrlResult` import 도 같은 식으로 (`com.atomiccv.resume.application.usecase` → `com.atomiccv.shared.application.usecase`).

- [ ] **Step 3: GetResumeUseCase import 갱신**

```diff
- import com.atomiccv.resume.application.port.S3Port
+ import com.atomiccv.shared.application.port.S3Port
```

- [ ] **Step 4: GetResumeUseCaseTest import 갱신**

```diff
- import com.atomiccv.resume.application.port.S3Port
+ import com.atomiccv.shared.application.port.S3Port
```

- [ ] **Step 5: ResumeControllerTest import 갱신**

```diff
- import com.atomiccv.resume.application.usecase.GenerateUploadUrlUseCase
+ import com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCase
- import com.atomiccv.resume.application.usecase.UploadUrlResult
+ import com.atomiccv.shared.application.usecase.UploadUrlResult
```

(grep 결과에 따라 실제 사용 import 만 변경. UploadUrlResult/Command 는 사용 시에만)

- [ ] **Step 6: 컴파일 확인**

Run: `./gradlew :module-resume:compileKotlin :module-resume:compileTestKotlin`
Expected: `BUILD SUCCESSFUL` — 이 단계까지는 module-resume 의 원본 파일이 살아있으나 unused 상태로 컴파일 통과.

---

## Task 7: module-resume 의 원본 파일 삭제

**Files:**
- Delete: `module-resume/src/main/kotlin/com/atomiccv/resume/application/port/S3Port.kt`
- Delete: `module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GenerateUploadUrlUseCase.kt`
- Delete: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/s3/S3Adapter.kt`
- Delete: `module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/s3/` (빈 디렉토리)

- [ ] **Step 1: 파일 삭제**

Run:
```bash
rm module-resume/src/main/kotlin/com/atomiccv/resume/application/port/S3Port.kt
rm module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GenerateUploadUrlUseCase.kt
rm module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/s3/S3Adapter.kt
rmdir module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/s3
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :module-resume:compileKotlin :module-resume:compileTestKotlin`
Expected: `BUILD SUCCESSFUL`. 만약 unresolved reference 가 나오면 Task 6 에서 빠뜨린 import 가 있는 것 — 해당 위치를 찾아 `com.atomiccv.shared.*` 로 갱신.

---

## Task 8: GenerateUploadUrlUseCaseTest 이동

**Files:**
- Create: `module-shared/src/test/kotlin/com/atomiccv/shared/application/usecase/GenerateUploadUrlUseCaseTest.kt`
- Delete: `module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GenerateUploadUrlUseCaseTest.kt`

- [ ] **Step 1: module-shared 에 새 파일 생성**

전체 내용을 그대로 작성 (패키지 선언과 S3Port import 만 새 위치로):

```kotlin
package com.atomiccv.shared.application.usecase

import com.atomiccv.shared.application.port.S3Port
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateUploadUrlUseCaseTest {
    private val s3Port: S3Port = mockk()
    private val useCase = GenerateUploadUrlUseCase(s3Port)

    @Test
    fun `presigned URL 발급 시 s3Key에 userId가 포함된다`() {
        every { s3Port.generateUploadPresignedUrl(any(), any()) } returns "https://s3.example.com/test"

        val result = useCase.generate(GenerateUploadUrlCommand(userId = 42L, fileName = "resume.pdf"))

        assertTrue(result.s3Key.contains("42"))
    }

    @Test
    fun `반환된 s3Key 형식이 resumes_{userId}_로 시작한다`() {
        every { s3Port.generateUploadPresignedUrl(any(), any()) } returns "https://s3.example.com/test"

        val result = useCase.generate(GenerateUploadUrlCommand(userId = 7L, fileName = "my-cv.pdf"))

        assertTrue(result.s3Key.startsWith("resumes/7/"))
    }

    @Test
    fun `presignedUrl은 s3Port에서 반환된 값과 동일하다`() {
        val expectedUrl = "https://s3.example.com/presigned-url"
        every { s3Port.generateUploadPresignedUrl(any(), any()) } returns expectedUrl

        val result = useCase.generate(GenerateUploadUrlCommand(userId = 1L, fileName = "resume.pdf"))

        assertEquals(expectedUrl, result.presignedUrl)
    }

    @Test
    fun `s3Key에 fileName이 포함된다`() {
        every { s3Port.generateUploadPresignedUrl(any(), any()) } returns "https://s3.example.com/test"

        val result = useCase.generate(GenerateUploadUrlCommand(userId = 1L, fileName = "my-resume.pdf"))

        assertTrue(result.s3Key.contains("my-resume.pdf"))
    }
}
```

- [ ] **Step 2: module-resume 의 원본 테스트 삭제**

Run: `rm module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GenerateUploadUrlUseCaseTest.kt`

- [ ] **Step 3: module-shared 테스트 실행**

Run: `./gradlew :module-shared:test --tests "com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCaseTest"`
Expected: PASS (테스트 케이스 수는 원본과 동일).

---

## Task 9: module-resume build.gradle.kts 의 AWS SDK 의존성 제거

**Files:**
- Modify: `module-resume/build.gradle.kts`

- [ ] **Step 1: 의존성 제거**

`module-resume/build.gradle.kts` 의 `dependencies { ... }` 블록에서 다음 두 줄 삭제:

```kotlin
implementation(platform("software.amazon.awssdk:bom:2.26.12"))
implementation("software.amazon.awssdk:s3")
```

- [ ] **Step 2: 의존성 해석 + 컴파일 확인**

Run: `./gradlew :module-resume:compileKotlin :module-resume:compileTestKotlin`
Expected: `BUILD SUCCESSFUL`. S3 SDK 직접 의존은 제거됐지만 `module-shared` 가 runtime 클래스패스로 제공하므로 Adapter 실행은 정상.

---

## Task 10: doc/MODULE_STRUCTURE.md 갱신

**Files:**
- Modify: `doc/MODULE_STRUCTURE.md`

- [ ] **Step 1: `:module-shared` 섹션 갱신**

68~75행 부근의 `### :module-shared` 표에 행 추가하여 다음 형태로 변경:

```markdown
### :module-shared

| 패키지 | 내용 |
|--------|------|
| `common.response` | `ApiResponse<T>` 공통 래퍼 |
| `common.exception` | `BusinessException` base, 에러코드 enum |
| `common.util` | 공통 유틸리티 |
| `application.port` | `S3Port` — 파일 업로드/다운로드 presigned URL 인터페이스 |
| `application.usecase` | `GenerateUploadUrlUseCase` — S3 presigned upload URL 발급 (도메인 무관) |
| `infrastructure.storage` | `S3Adapter`, `SharedStorageConfiguration` — AWS S3 SDK 어댑터·Bean |
| `infrastructure.persistence` | `BaseJpaEntity` — JPA Auditing 베이스 |
| `interfaces.rest` | `GlobalExceptionHandler` — 공통 예외 응답 |

> 다른 모듈이 공통으로 의존하는 라이브러리만 포함. **도메인 로직 포함 금지** — 도메인 무관한 공용 인프라/UseCase 는 허용.
```

(기존 "비즈니스 로직 포함 금지" 문구를 "도메인 로직 포함 금지 — 도메인 무관한 공용 인프라/UseCase 는 허용" 으로 변경)

- [ ] **Step 2: `:module-resume` 섹션의 S3 책임 명시 검토**

`### :module-resume` 표에 S3 항목이 명시되어 있지 않으면 변경 불필요. 명시되어 있다면 제거.

Run: `grep -n "S3\|업로드" doc/MODULE_STRUCTURE.md`
변경이 필요한 라인이 있으면 수정, 없으면 다음 step.

---

## Task 11: 전체 lint + 테스트 검증

**Files:** 없음 (검증만)

- [ ] **Step 1: 전체 lint**

Run: `./gradlew ktlintCheck detekt 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

위반 발견 시:
- ktlint → `./gradlew ktlintFormat` 으로 자동 수정 후 재실행
- detekt → 수동 수정 (보통 MaxLineLength, UnusedImport 등)

- [ ] **Step 2: module-shared, module-resume 테스트**

Run: `./gradlew :module-shared:test :module-resume:test --continue 2>&1 | tail -15`
Expected: 둘 다 `BUILD SUCCESSFUL`. 실패 시 실패 케이스 확인 후 fix.

- [ ] **Step 3: 전체 테스트 (회귀 확인)**

Run: `./gradlew test --continue 2>&1 | tail -20`
Expected: `app:test` 의 환경 의존 실패(`cloud.aws.region.static` placeholder) 외에는 모두 통과.

---

## Task 12: 커밋 + 푸시 + PR

**Files:** 없음 (git 작업)

- [ ] **Step 1: deploy-precheck 토큰 발급**

이 작업은 코드 변경이며 secret/credential 미포함 (S3 SDK 코드만 이동). `.claude/.deploy-token-*` 신규 생성:

```bash
SHA=$(openssl rand -hex 6); touch ".claude/.deploy-token-${SHA}"
```

- [ ] **Step 2: 스테이징**

```bash
git add module-shared/build.gradle.kts \
        module-shared/src/main/kotlin/com/atomiccv/shared/application/port/S3Port.kt \
        module-shared/src/main/kotlin/com/atomiccv/shared/application/usecase/GenerateUploadUrlUseCase.kt \
        module-shared/src/main/kotlin/com/atomiccv/shared/infrastructure/storage/S3Adapter.kt \
        module-shared/src/main/kotlin/com/atomiccv/shared/infrastructure/storage/SharedStorageConfiguration.kt \
        module-shared/src/test/kotlin/com/atomiccv/shared/application/usecase/GenerateUploadUrlUseCaseTest.kt \
        module-resume/build.gradle.kts \
        module-resume/src/main/kotlin/com/atomiccv/resume/infrastructure/ResumeModuleConfiguration.kt \
        module-resume/src/main/kotlin/com/atomiccv/resume/interfaces/rest/ResumeController.kt \
        module-resume/src/main/kotlin/com/atomiccv/resume/application/usecase/GetResumeUseCase.kt \
        module-resume/src/test/kotlin/com/atomiccv/resume/application/usecase/GetResumeUseCaseTest.kt \
        module-resume/src/test/kotlin/com/atomiccv/resume/interfaces/rest/ResumeControllerTest.kt \
        doc/MODULE_STRUCTURE.md
git add -u  # 삭제된 파일들 반영
```

- [ ] **Step 3: 커밋**

```bash
git commit -m "$(cat <<'EOF'
refactor(shared): 이미지/파일 업로드 기능을 module-shared 로 이동

S3 presigned URL 기반 업로드 기능(S3Port / S3Adapter /
GenerateUploadUrlUseCase) 을 module-resume → module-shared 로 이동.
Block, Profile 등 다른 도메인 모듈이 재사용 가능한 위치로 이전.

- module-shared 에 application.port.S3Port, application.usecase.
  GenerateUploadUrlUseCase, infrastructure.storage.{S3Adapter,
  SharedStorageConfiguration} 추가
- AWS S3 SDK 의존성을 module-resume → module-shared 로 이전
- ResumeModuleConfiguration 의 S3 Bean 제거 (SharedStorageConfiguration
  으로 대체)
- ResumeController, GetResumeUseCase 등 caller 의 import 경로 갱신
- GenerateUploadUrlUseCaseTest 도 module-shared 로 이동
- doc/MODULE_STRUCTURE.md 의 module-shared 역할 갱신

엔드포인트 (POST /api/resumes/upload-url), DTO, key prefix 모두 변경
없음. Block/Profile 도메인의 자기 endpoint 추가 및 key prefix 일반화는
별도 PR 에서 진행.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 4: 푸시**

```bash
git push -u origin HEAD
```

- [ ] **Step 5: PR 생성**

```bash
gh pr create --base dev \
  --title "refactor(shared): 이미지/파일 업로드 기능을 module-shared 로 이동" \
  --body "$(cat <<'EOF'
## Summary
S3 presigned URL 기반 업로드 기능을 \`module-resume\` 에서 \`module-shared\` 로 이동. Block, Profile 등 다른 도메인 모듈이 재사용 가능하게 추출. 엔드포인트·DTO·key prefix 그대로.

## 변경
- \`S3Port\`, \`GenerateUploadUrlUseCase\` (+Command+Result), \`S3Adapter\` 를 \`module-shared\` 로 이동
- \`SharedStorageConfiguration\` 신규 — S3Presigner / S3Adapter / GenerateUploadUrlUseCase Bean 등록
- AWS S3 SDK 의존성을 \`module-resume\` → \`module-shared\` 로 이전
- \`ResumeModuleConfiguration\` 의 S3 관련 Bean 제거
- \`ResumeController\`, \`GetResumeUseCase\` 등 caller import 갱신
- \`GenerateUploadUrlUseCaseTest\` 이동
- \`doc/MODULE_STRUCTURE.md\` 갱신 — \`:module-shared\` 책임에 storage / GenerateUploadUrlUseCase 추가

## 후속 작업 (별도 PR)
- \`BlockController\` 에 자기 \`upload-url\` 엔드포인트 추가
- \`GenerateUploadUrlCommand\` 의 key prefix 일반화 (도메인별 context 파라미터)
- Profile 이미지 업로드 endpoint (필요 시)
- \`resume.s3.bucket-name\` 프로퍼티 키 일반화

## Test Plan
- [x] \`module-shared:test\` — GenerateUploadUrlUseCaseTest 통과
- [x] \`module-resume:test\` — 회귀 통과 (ResumeControllerTest 의 upload-url 케이스 포함)
- [x] 전체 \`ktlintCheck + detekt\` 통과
- [ ] dev 배포 후 \`POST /api/resumes/upload-url\` 호출 시 presigned URL 정상 발급 확인

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## 완료 체크리스트

- [ ] `module-resume` 안에 S3 관련 파일·의존성 없음
- [ ] `module-shared:test` 와 `module-resume:test` 모두 통과
- [ ] `POST /api/resumes/upload-url` 동작·DTO·경로 변경 전과 동일
- [ ] 전체 ktlint + detekt 통과
- [ ] `doc/MODULE_STRUCTURE.md` 의 `:module-shared` 항목 갱신됨
- [ ] PR 생성됨
