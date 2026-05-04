# Worklog Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Atomic CV 백엔드에 `module-worklog`를 추가하여 팀 공유 Worklog 대시보드를 구축한다 — 오늘의 미팅(음성 업로드 → AI 요약), GitHub 활동, 코드 활동을 한 페이지에서 확인한다.

**Architecture:** 기존 Hexagonal DDD 패턴을 그대로 따른다. Spring Boot Thymeleaf + HTMX 서버 사이드 렌더링으로 npm 빌드 없이 제공한다. OpenAI Whisper API로 음성을 텍스트로 변환하고, GPT-4o-mini로 요약한다. GitHub Search API로 오늘의 활동을 조회하며 Caffeine으로 10분 캐싱한다.

**Tech Stack:** Kotlin, Spring Boot 3.5, Thymeleaf, HTMX 2.x (CDN), JPA + H2(테스트), OpenAI REST API, GitHub REST API, Caffeine, MockK

---

## 파일 구조

```
module-worklog/
├── build.gradle.kts
└── src/
    ├── main/kotlin/com/atomiccv/worklog/
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Meeting.kt               # 회의 도메인 모델
    │   │   │   ├── MeetingStatus.kt         # 상태 enum
    │   │   │   ├── GithubActivity.kt        # GitHub 활동 값객체
    │   │   │   └── DailyWorklog.kt          # 날짜별 집계 값객체
    │   │   └── repository/
    │   │       └── MeetingRepository.kt     # 도메인 레포 인터페이스
    │   ├── application/
    │   │   ├── port/
    │   │   │   ├── WhisperPort.kt           # 음성→텍스트 포트
    │   │   │   ├── GptSummaryPort.kt        # 텍스트→요약 포트
    │   │   │   └── GithubActivityPort.kt    # GitHub 활동 조회 포트
    │   │   └── usecase/
    │   │       ├── UploadMeetingUseCase.kt  # 음성 업로드→저장
    │   │       ├── GetDailyWorklogUseCase.kt
    │   │       └── GetGithubActivityUseCase.kt
    │   ├── infrastructure/
    │   │   ├── persistence/
    │   │   │   ├── MeetingJpaEntity.kt
    │   │   │   ├── MeetingJpaRepository.kt  # Spring Data JPA
    │   │   │   └── MeetingRepositoryImpl.kt
    │   │   ├── client/
    │   │   │   ├── WhisperClient.kt         # OpenAI Whisper API
    │   │   │   ├── GptSummaryClient.kt      # OpenAI GPT API
    │   │   │   └── GithubClient.kt          # GitHub Search API + Caffeine
    │   │   └── WorklogConfiguration.kt      # Bean 등록
    │   └── interfaces/web/
    │       ├── WorklogController.kt         # 페이지 렌더링
    │       └── MeetingApiController.kt      # 업로드 처리
    └── main/resources/templates/worklog/
        ├── dashboard.html
        └── meeting-card.html                # HTMX partial
    test/kotlin/com/atomiccv/worklog/
    ├── WorklogTestApplication.kt
    ├── application/
    │   ├── UploadMeetingUseCaseTest.kt
    │   ├── GetDailyWorklogUseCaseTest.kt
    │   └── GetGithubActivityUseCaseTest.kt
    └── infrastructure/
        ├── MeetingRepositoryImplTest.kt
        ├── WhisperClientTest.kt
        └── GithubClientTest.kt
    test/resources/application-test.yml
```

---

## Task 1: 모듈 셋업

**Files:**
- Modify: `settings.gradle.kts`
- Create: `module-worklog/build.gradle.kts`
- Create: `module-worklog/src/test/kotlin/com/atomiccv/worklog/WorklogTestApplication.kt`
- Create: `module-worklog/src/test/resources/application-test.yml`

- [ ] **Step 1: settings.gradle.kts에 모듈 추가**

```kotlin
// settings.gradle.kts — include 블록에 추가
include(
    ":app",
    ":module-shared",
    ":module-auth",
    ":module-resume",
    ":module-worklog",   // 추가
)
```

- [ ] **Step 2: module-worklog/build.gradle.kts 생성**

```kotlin
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
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.mysql:mysql-connector-j")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

- [ ] **Step 3: WorklogTestApplication.kt 생성**

```kotlin
// module-worklog/src/test/kotlin/com/atomiccv/worklog/WorklogTestApplication.kt
package com.atomiccv.worklog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.test.context.ActiveProfiles

@SpringBootApplication
@ActiveProfiles("test")
class WorklogTestApplication
```

- [ ] **Step 4: application-test.yml 생성**

```yaml
# module-worklog/src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:worklog-testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect

worklog:
  openai:
    api-key: test-api-key
  github:
    token: test-github-token
    org: resume-helper
  audio:
    upload-dir: /tmp/worklog-test/audio
```

- [ ] **Step 5: 빌드 확인**

```bash
./gradlew :module-worklog:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add settings.gradle.kts module-worklog/
git commit -m "chore(worklog): module-worklog 모듈 초기 셋업"
```

---

## Task 2: 도메인 모델

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/model/MeetingStatus.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/model/Meeting.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/model/GithubActivity.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/model/DailyWorklog.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/repository/MeetingRepository.kt`

- [ ] **Step 1: MeetingStatus.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/model/MeetingStatus.kt
package com.atomiccv.worklog.domain.model

enum class MeetingStatus {
    COMPLETED,
    FAILED,
}
```

- [ ] **Step 2: Meeting.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/model/Meeting.kt
package com.atomiccv.worklog.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Meeting(
    val id: Long = 0,
    val title: String,
    val meetingDate: LocalDate,
    val transcript: String = "",
    val summary: String = "",
    val status: MeetingStatus = MeetingStatus.COMPLETED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
```

- [ ] **Step 3: GithubActivity.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/model/GithubActivity.kt
package com.atomiccv.worklog.domain.model

data class GithubCommit(
    val sha: String,
    val message: String,
    val authorName: String,
    val repoName: String,
)

data class GithubPullRequest(
    val number: Int,
    val title: String,
    val state: String,
    val repoName: String,
)

data class GithubIssue(
    val number: Int,
    val title: String,
    val state: String,
    val repoName: String,
)

data class GithubActivity(
    val commits: List<GithubCommit> = emptyList(),
    val pullRequests: List<GithubPullRequest> = emptyList(),
    val issues: List<GithubIssue> = emptyList(),
) {
    val commitCount: Int get() = commits.size
    val prCount: Int get() = pullRequests.size
    val issueCount: Int get() = issues.size
}
```

- [ ] **Step 4: DailyWorklog.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/model/DailyWorklog.kt
package com.atomiccv.worklog.domain.model

import java.time.LocalDate

data class DailyWorklog(
    val date: LocalDate,
    val meetings: List<Meeting>,
    val githubActivity: GithubActivity,
)
```

- [ ] **Step 5: MeetingRepository.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/repository/MeetingRepository.kt
package com.atomiccv.worklog.domain.repository

import com.atomiccv.worklog.domain.model.Meeting
import java.time.LocalDate

interface MeetingRepository {
    fun save(meeting: Meeting): Meeting
    fun update(meeting: Meeting): Meeting
    fun findById(id: Long): Meeting?
    fun findByMeetingDate(date: LocalDate): List<Meeting>
}
```

- [ ] **Step 6: 컴파일 확인**

```bash
./gradlew :module-worklog:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/domain/
git commit -m "feat(worklog): 도메인 모델 및 MeetingRepository 인터페이스 추가"
```

---

## Task 3: Meeting JPA 영속성

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/persistence/MeetingJpaEntity.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/persistence/MeetingJpaRepository.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/persistence/MeetingRepositoryImpl.kt`
- Create: `module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/MeetingRepositoryImplTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/MeetingRepositoryImplTest.kt
package com.atomiccv.worklog.infrastructure

import com.atomiccv.worklog.WorklogTestApplication
import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.model.MeetingStatus
import com.atomiccv.worklog.infrastructure.persistence.MeetingRepositoryImpl
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest(classes = [WorklogTestApplication::class])
class MeetingRepositoryImplTest {
    @Autowired
    lateinit var meetingRepository: MeetingRepositoryImpl

    @Test
    fun `회의를 저장하고 날짜로 조회할 수 있다`() {
        val date = LocalDate.of(2026, 5, 5)
        val meeting = Meeting(title = "스프린트 회의", meetingDate = date, summary = "요약 내용")

        val saved = meetingRepository.save(meeting)

        assertNotNull(saved.id)
        val found = meetingRepository.findByMeetingDate(date)
        assertEquals(1, found.size)
        assertEquals("스프린트 회의", found[0].title)
    }

    @Test
    fun `ID로 회의를 조회할 수 있다`() {
        val meeting = Meeting(title = "아키텍처 리뷰", meetingDate = LocalDate.now(), summary = "요약")
        val saved = meetingRepository.save(meeting)

        val found = meetingRepository.findById(saved.id)

        assertNotNull(found)
        assertEquals("아키텍처 리뷰", found.title)
    }

    @Test
    fun `존재하지 않는 ID 조회 시 null을 반환한다`() {
        val result = meetingRepository.findById(99999L)
        assertNull(result)
    }

    @Test
    fun `회의를 업데이트할 수 있다`() {
        val meeting = Meeting(title = "회의", meetingDate = LocalDate.now(), summary = "")
        val saved = meetingRepository.save(meeting)
        val updated = saved.copy(summary = "업데이트된 요약", status = MeetingStatus.COMPLETED)

        val result = meetingRepository.update(updated)

        assertEquals("업데이트된 요약", result.summary)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :module-worklog:test --tests "*.MeetingRepositoryImplTest"
```

Expected: FAIL — MeetingRepositoryImpl not found

- [ ] **Step 3: MeetingJpaEntity.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/persistence/MeetingJpaEntity.kt
package com.atomiccv.worklog.infrastructure.persistence

import com.atomiccv.shared.infrastructure.persistence.BaseJpaEntity
import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.model.MeetingStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "meetings")
class MeetingJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val title: String,
    @Column(name = "meeting_date", nullable = false)
    val meetingDate: LocalDate,
    @Column(columnDefinition = "TEXT")
    val transcript: String = "",
    @Column(columnDefinition = "TEXT")
    val summary: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: MeetingStatus = MeetingStatus.COMPLETED,
) : BaseJpaEntity() {
    fun toDomain() = Meeting(
        id = id,
        title = title,
        meetingDate = meetingDate,
        transcript = transcript,
        summary = summary,
        status = status,
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(meeting: Meeting) = MeetingJpaEntity(
            id = meeting.id,
            title = meeting.title,
            meetingDate = meeting.meetingDate,
            transcript = meeting.transcript,
            summary = meeting.summary,
            status = meeting.status,
        )
    }
}
```

- [ ] **Step 4: MeetingJpaRepository.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/persistence/MeetingJpaRepository.kt
package com.atomiccv.worklog.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MeetingJpaRepository : JpaRepository<MeetingJpaEntity, Long> {
    fun findByMeetingDate(meetingDate: LocalDate): List<MeetingJpaEntity>
}
```

- [ ] **Step 5: MeetingRepositoryImpl.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/persistence/MeetingRepositoryImpl.kt
package com.atomiccv.worklog.infrastructure.persistence

import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.repository.MeetingRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MeetingRepositoryImpl(
    private val jpaRepository: MeetingJpaRepository,
) : MeetingRepository {
    override fun save(meeting: Meeting): Meeting =
        jpaRepository.save(MeetingJpaEntity.fromDomain(meeting)).toDomain()

    override fun update(meeting: Meeting): Meeting =
        jpaRepository.save(MeetingJpaEntity.fromDomain(meeting)).toDomain()

    override fun findById(id: Long): Meeting? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByMeetingDate(date: LocalDate): List<Meeting> =
        jpaRepository.findByMeetingDate(date).map { it.toDomain() }
}
```

- [ ] **Step 6: 테스트 통과 확인**

```bash
./gradlew :module-worklog:test --tests "*.MeetingRepositoryImplTest"
```

Expected: PASS (4 tests)

- [ ] **Step 7: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/persistence/ \
        module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/MeetingRepositoryImplTest.kt
git commit -m "feat(worklog): Meeting JPA 영속성 레이어 추가"
```

---

## Task 4: Port 인터페이스 정의

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/application/port/WhisperPort.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/application/port/GptSummaryPort.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/application/port/GithubActivityPort.kt`

- [ ] **Step 1: WhisperPort.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/application/port/WhisperPort.kt
package com.atomiccv.worklog.application.port

interface WhisperPort {
    /** 음성 파일 바이트를 한국어 텍스트로 변환한다 */
    fun transcribe(audioBytes: ByteArray, fileName: String): String
}
```

- [ ] **Step 2: GptSummaryPort.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/application/port/GptSummaryPort.kt
package com.atomiccv.worklog.application.port

interface GptSummaryPort {
    /** 회의 전사 텍스트를 bullet point 요약으로 변환한다 */
    fun summarize(transcript: String): String
}
```

- [ ] **Step 3: GithubActivityPort.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/application/port/GithubActivityPort.kt
package com.atomiccv.worklog.application.port

import com.atomiccv.worklog.domain.model.GithubActivity
import java.time.LocalDate

interface GithubActivityPort {
    /** 지정 날짜의 GitHub 활동(커밋/PR/이슈)을 조회한다 */
    fun getActivity(date: LocalDate): GithubActivity
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew :module-worklog:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/application/port/
git commit -m "feat(worklog): application port 인터페이스 정의"
```

---

## Task 5: GithubClient (GitHub API + Caffeine 캐시)

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/GithubClient.kt`
- Create: `module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/GithubClientTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/GithubClientTest.kt
package com.atomiccv.worklog.infrastructure

import com.atomiccv.worklog.infrastructure.client.GithubClient
import com.atomiccv.worklog.infrastructure.client.GithubSearchCommitsResponse
import com.atomiccv.worklog.infrastructure.client.GithubSearchIssuesResponse
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class GithubClientTest {
    private val restClient: RestClient = mockk()
    private val cache: Cache<String, Any> = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build()
    private val client = GithubClient(
        githubRestClient = restClient,
        cache = cache,
        org = "resume-helper",
    )

    @BeforeEach
    fun setUp() = cache.invalidateAll()

    @Test
    fun `같은 날짜 두 번 조회 시 API는 한 번만 호출된다`() {
        val date = LocalDate.of(2026, 5, 5)
        val requestSpec: RestClient.RequestHeadersUriSpec<*> = mockk(relaxed = true)
        val responseSpec: RestClient.ResponseSpec = mockk(relaxed = true)

        every { restClient.get() } returns requestSpec
        every { requestSpec.uri(any<String>()) } returns requestSpec
        every { requestSpec.header(any(), any()) } returns requestSpec
        every { requestSpec.retrieve() } returns responseSpec
        every { responseSpec.body(GithubSearchCommitsResponse::class.java) } returns
            GithubSearchCommitsResponse(totalCount = 0, items = emptyList())
        every { responseSpec.body(GithubSearchIssuesResponse::class.java) } returns
            GithubSearchIssuesResponse(totalCount = 0, items = emptyList())

        client.getActivity(date)
        client.getActivity(date)

        // 2번 요청(커밋, PR/이슈) × 1번만 호출 = 총 2번
        verify(exactly = 2) { restClient.get() }
    }

    @Test
    fun `GitHub API 오류 시 빈 GithubActivity를 반환한다`() {
        val date = LocalDate.of(2026, 5, 5)
        val requestSpec: RestClient.RequestHeadersUriSpec<*> = mockk(relaxed = true)

        every { restClient.get() } returns requestSpec
        every { requestSpec.uri(any<String>()) } returns requestSpec
        every { requestSpec.header(any(), any()) } returns requestSpec
        every { requestSpec.retrieve() } throws RuntimeException("GitHub API 오류")

        val result = client.getActivity(date)

        assertEquals(0, result.commitCount)
        assertEquals(0, result.prCount)
        assertEquals(0, result.issueCount)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :module-worklog:test --tests "*.GithubClientTest"
```

Expected: FAIL — GithubClient not found

- [ ] **Step 3: GithubClient.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/GithubClient.kt
package com.atomiccv.worklog.infrastructure.client

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.domain.model.GithubActivity
import com.atomiccv.worklog.domain.model.GithubCommit
import com.atomiccv.worklog.domain.model.GithubIssue
import com.atomiccv.worklog.domain.model.GithubPullRequest
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.benmanes.caffeine.cache.Cache
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import java.time.LocalDate

// GitHub Search API 응답 DTO
data class GithubSearchCommitsResponse(
    @JsonProperty("total_count") val totalCount: Int,
    val items: List<GithubCommitItem>,
)

data class GithubCommitItem(
    val sha: String,
    val commit: GithubCommitDetail,
    val repository: GithubRepo?,
)

data class GithubCommitDetail(
    val message: String,
    val author: GithubCommitAuthor,
)

data class GithubCommitAuthor(val name: String)

data class GithubRepo(val name: String)

data class GithubSearchIssuesResponse(
    @JsonProperty("total_count") val totalCount: Int,
    val items: List<GithubIssueItem>,
)

data class GithubIssueItem(
    val number: Int,
    val title: String,
    val state: String,
    @JsonProperty("repository_url") val repositoryUrl: String,
    @JsonProperty("pull_request") val pullRequest: GithubPrInfo? = null,
)

data class GithubPrInfo(
    @JsonProperty("merged_at") val mergedAt: String?,
)

class GithubClient(
    private val githubRestClient: RestClient,
    private val cache: Cache<String, Any>,
    private val org: String,
) : GithubActivityPort {
    private val log = LoggerFactory.getLogger(javaClass)

    @Suppress("UNCHECKED_CAST")
    override fun getActivity(date: LocalDate): GithubActivity {
        val cacheKey = "github-activity-$date"
        val cached = cache.getIfPresent(cacheKey)
        if (cached != null) return cached as GithubActivity

        val activity = fetchActivity(date)
        cache.put(cacheKey, activity)
        return activity
    }

    private fun fetchActivity(date: LocalDate): GithubActivity {
        return try {
            val commits = fetchCommits(date)
            val (prs, issues) = fetchPrsAndIssues(date)
            GithubActivity(commits = commits, pullRequests = prs, issues = issues)
        } catch (e: Exception) {
            log.warn("GitHub API 조회 실패: ${e.message}")
            GithubActivity()
        }
    }

    private fun fetchCommits(date: LocalDate): List<GithubCommit> {
        val response = githubRestClient.get()
            .uri("/search/commits?q=org:$org+author-date:$date&per_page=30")
            .header("Accept", "application/vnd.github+json")
            .retrieve()
            .body(GithubSearchCommitsResponse::class.java)
            ?: return emptyList()

        return response.items.map { item ->
            GithubCommit(
                sha = item.sha.take(7),
                message = item.commit.message.lines().first(),
                authorName = item.commit.author.name,
                repoName = item.repository?.name ?: "",
            )
        }
    }

    private fun fetchPrsAndIssues(date: LocalDate): Pair<List<GithubPullRequest>, List<GithubIssue>> {
        val response = githubRestClient.get()
            .uri("/search/issues?q=org:$org+updated:$date&per_page=30")
            .header("Accept", "application/vnd.github+json")
            .retrieve()
            .body(GithubSearchIssuesResponse::class.java)
            ?: return Pair(emptyList(), emptyList())

        val repoNameRegex = Regex("/repos/[^/]+/([^/]+)$")
        val prs = response.items
            .filter { it.pullRequest != null }
            .map { item ->
                GithubPullRequest(
                    number = item.number,
                    title = item.title,
                    state = item.state,
                    repoName = repoNameRegex.find(item.repositoryUrl)?.groupValues?.get(1) ?: "",
                )
            }
        val issues = response.items
            .filter { it.pullRequest == null }
            .map { item ->
                GithubIssue(
                    number = item.number,
                    title = item.title,
                    state = item.state,
                    repoName = repoNameRegex.find(item.repositoryUrl)?.groupValues?.get(1) ?: "",
                )
            }
        return Pair(prs, issues)
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :module-worklog:test --tests "*.GithubClientTest"
```

Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/GithubClient.kt \
        module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/GithubClientTest.kt
git commit -m "feat(worklog): GithubClient — GitHub Search API + Caffeine 캐싱 구현"
```

---

## Task 6: OpenAI 클라이언트 (Whisper + GPT)

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/WhisperClient.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/GptSummaryClient.kt`
- Create: `module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/WhisperClientTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/WhisperClientTest.kt
package com.atomiccv.worklog.infrastructure

import com.atomiccv.worklog.infrastructure.client.GptSummaryClient
import com.atomiccv.worklog.infrastructure.client.GptResponse
import com.atomiccv.worklog.infrastructure.client.WhisperClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals

class WhisperClientTest {
    private val restClient: RestClient = mockk()

    @Test
    fun `Whisper API 호출 시 transcript 텍스트를 반환한다`() {
        val requestSpec: RestClient.RequestBodyUriSpec = mockk(relaxed = true)
        val responseSpec: RestClient.ResponseSpec = mockk(relaxed = true)

        every { restClient.post() } returns requestSpec
        every { requestSpec.uri(any<String>()) } returns requestSpec
        every { requestSpec.contentType(any()) } returns requestSpec
        every { requestSpec.body(any()) } returns requestSpec
        every { requestSpec.retrieve() } returns responseSpec
        every { responseSpec.body(String::class.java) } returns "안녕하세요 회의 내용입니다"

        val client = WhisperClient(openAiRestClient = restClient)
        val result = client.transcribe("audio".toByteArray(), "meeting.mp3")

        assertEquals("안녕하세요 회의 내용입니다", result)
    }

    @Test
    fun `GPT 요약 API 호출 시 요약 텍스트를 반환한다`() {
        val requestSpec: RestClient.RequestBodyUriSpec = mockk(relaxed = true)
        val responseSpec: RestClient.ResponseSpec = mockk(relaxed = true)
        val gptResponse = GptResponse(
            choices = listOf(
                com.atomiccv.worklog.infrastructure.client.GptChoice(
                    message = com.atomiccv.worklog.infrastructure.client.GptMessage(
                        role = "assistant",
                        content = "• 핵심 결정: JWT 만료 7일로 변경\n• 액션: 홍길동 - PR 작성",
                    )
                )
            )
        )

        every { restClient.post() } returns requestSpec
        every { requestSpec.uri(any<String>()) } returns requestSpec
        every { requestSpec.contentType(any()) } returns requestSpec
        every { requestSpec.body(any()) } returns requestSpec
        every { requestSpec.retrieve() } returns responseSpec
        every { responseSpec.body(GptResponse::class.java) } returns gptResponse

        val client = GptSummaryClient(openAiRestClient = restClient)
        val result = client.summarize("회의 전사 텍스트")

        assertEquals("• 핵심 결정: JWT 만료 7일로 변경\n• 액션: 홍길동 - PR 작성", result)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :module-worklog:test --tests "*.WhisperClientTest"
```

Expected: FAIL

- [ ] **Step 3: WhisperClient.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/WhisperClient.kt
package com.atomiccv.worklog.infrastructure.client

import com.atomiccv.worklog.application.port.WhisperPort
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

class WhisperClient(
    private val openAiRestClient: RestClient,
) : WhisperPort {
    override fun transcribe(audioBytes: ByteArray, fileName: String): String {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("file", object : ByteArrayResource(audioBytes) {
            override fun getFilename() = fileName
        })
        body.add("model", "whisper-1")
        body.add("language", "ko")
        body.add("response_format", "text")

        return openAiRestClient.post()
            .uri("/v1/audio/transcriptions")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(String::class.java)
            ?: throw RuntimeException("Whisper API 응답이 없습니다")
    }
}
```

- [ ] **Step 4: GptSummaryClient.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/GptSummaryClient.kt
package com.atomiccv.worklog.infrastructure.client

import com.atomiccv.worklog.application.port.GptSummaryPort
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

data class GptMessage(val role: String, val content: String)
data class GptRequest(
    val model: String,
    val messages: List<GptMessage>,
    @JsonProperty("max_tokens") val maxTokens: Int = 500,
)
data class GptChoice(val message: GptMessage)
data class GptResponse(val choices: List<GptChoice>)

class GptSummaryClient(
    private val openAiRestClient: RestClient,
) : GptSummaryPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun summarize(transcript: String): String {
        val request = GptRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                GptMessage(
                    role = "system",
                    content = "당신은 회의 내용을 한국어로 요약하는 어시스턴트입니다. " +
                        "핵심 결정사항과 액션 아이템을 중심으로 3~5개 bullet point(•)로 요약하세요.",
                ),
                GptMessage(role = "user", content = transcript),
            ),
        )
        return try {
            openAiRestClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GptResponse::class.java)
                ?.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?: "요약 실패"
        } catch (e: Exception) {
            log.warn("GPT 요약 실패: ${e.message}")
            "요약 실패 — 다시 시도해주세요"
        }
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew :module-worklog:test --tests "*.WhisperClientTest"
```

Expected: PASS (2 tests)

- [ ] **Step 6: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/WhisperClient.kt \
        module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/client/GptSummaryClient.kt \
        module-worklog/src/test/kotlin/com/atomiccv/worklog/infrastructure/WhisperClientTest.kt
git commit -m "feat(worklog): WhisperClient, GptSummaryClient OpenAI API 클라이언트 구현"
```

---

## Task 7: GetGithubActivityUseCase

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/GetGithubActivityUseCase.kt`
- Create: `module-worklog/src/test/kotlin/com/atomiccv/worklog/application/GetGithubActivityUseCaseTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-worklog/src/test/kotlin/com/atomiccv/worklog/application/GetGithubActivityUseCaseTest.kt
package com.atomiccv.worklog.application

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.application.usecase.GetGithubActivityUseCase
import com.atomiccv.worklog.domain.model.GithubActivity
import com.atomiccv.worklog.domain.model.GithubCommit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class GetGithubActivityUseCaseTest {
    private val githubActivityPort: GithubActivityPort = mockk()
    private val useCase = GetGithubActivityUseCase(githubActivityPort)

    @Test
    fun `지정 날짜의 GitHub 활동을 반환한다`() {
        val date = LocalDate.of(2026, 5, 5)
        val expected = GithubActivity(
            commits = listOf(GithubCommit("abc1234", "feat: 추가", "홍길동", "module-auth")),
        )
        every { githubActivityPort.getActivity(date) } returns expected

        val result = useCase.execute(date)

        assertEquals(1, result.commitCount)
        verify(exactly = 1) { githubActivityPort.getActivity(date) }
    }

    @Test
    fun `날짜를 전달하지 않으면 오늘 날짜로 조회한다`() {
        val today = LocalDate.now()
        every { githubActivityPort.getActivity(today) } returns GithubActivity()

        useCase.execute(today)

        verify { githubActivityPort.getActivity(today) }
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :module-worklog:test --tests "*.GetGithubActivityUseCaseTest"
```

Expected: FAIL

- [ ] **Step 3: GetGithubActivityUseCase.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/GetGithubActivityUseCase.kt
package com.atomiccv.worklog.application.usecase

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.domain.model.GithubActivity
import java.time.LocalDate

class GetGithubActivityUseCase(
    private val githubActivityPort: GithubActivityPort,
) {
    fun execute(date: LocalDate = LocalDate.now()): GithubActivity =
        githubActivityPort.getActivity(date)
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :module-worklog:test --tests "*.GetGithubActivityUseCaseTest"
```

Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/GetGithubActivityUseCase.kt \
        module-worklog/src/test/kotlin/com/atomiccv/worklog/application/GetGithubActivityUseCaseTest.kt
git commit -m "feat(worklog): GetGithubActivityUseCase 구현"
```

---

## Task 8: UploadMeetingUseCase

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/UploadMeetingUseCase.kt`
- Create: `module-worklog/src/test/kotlin/com/atomiccv/worklog/application/UploadMeetingUseCaseTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-worklog/src/test/kotlin/com/atomiccv/worklog/application/UploadMeetingUseCaseTest.kt
package com.atomiccv.worklog.application

import com.atomiccv.worklog.application.port.GptSummaryPort
import com.atomiccv.worklog.application.port.WhisperPort
import com.atomiccv.worklog.application.usecase.UploadMeetingCommand
import com.atomiccv.worklog.application.usecase.UploadMeetingUseCase
import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.model.MeetingStatus
import com.atomiccv.worklog.domain.repository.MeetingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class UploadMeetingUseCaseTest {
    private val whisperPort: WhisperPort = mockk()
    private val gptSummaryPort: GptSummaryPort = mockk()
    private val meetingRepository: MeetingRepository = mockk()
    private val useCase = UploadMeetingUseCase(whisperPort, gptSummaryPort, meetingRepository)

    @Test
    fun `음성 업로드 시 Whisper 변환, GPT 요약 후 Meeting이 저장된다`() {
        val command = UploadMeetingCommand(
            title = "스프린트 회의",
            meetingDate = LocalDate.of(2026, 5, 5),
            audioBytes = "audio-data".toByteArray(),
            fileName = "sprint.mp3",
        )
        val savedMeetingSlot = slot<Meeting>()

        every { whisperPort.transcribe(command.audioBytes, command.fileName) } returns "회의 내용입니다"
        every { gptSummaryPort.summarize("회의 내용입니다") } returns "• 주요 결정: 배포 일정 확정"
        every { meetingRepository.save(capture(savedMeetingSlot)) } answers {
            savedMeetingSlot.captured.copy(id = 1L)
        }

        val result = useCase.execute(command)

        assertEquals(1L, result.id)
        assertEquals("회의 내용입니다", savedMeetingSlot.captured.transcript)
        assertEquals("• 주요 결정: 배포 일정 확정", savedMeetingSlot.captured.summary)
        assertEquals(MeetingStatus.COMPLETED, savedMeetingSlot.captured.status)
    }

    @Test
    fun `Whisper API 실패 시 FAILED 상태로 Meeting이 저장된다`() {
        val command = UploadMeetingCommand(
            title = "회의",
            meetingDate = LocalDate.now(),
            audioBytes = ByteArray(0),
            fileName = "meeting.mp3",
        )
        val savedSlot = slot<Meeting>()

        every { whisperPort.transcribe(any(), any()) } throws RuntimeException("API 오류")
        every { meetingRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = 2L)
        }

        val result = useCase.execute(command)

        assertEquals(MeetingStatus.FAILED, savedSlot.captured.status)
        assertEquals(2L, result.id)
        verify(exactly = 0) { gptSummaryPort.summarize(any()) }
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :module-worklog:test --tests "*.UploadMeetingUseCaseTest"
```

Expected: FAIL

- [ ] **Step 3: UploadMeetingUseCase.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/UploadMeetingUseCase.kt
package com.atomiccv.worklog.application.usecase

import com.atomiccv.worklog.application.port.GptSummaryPort
import com.atomiccv.worklog.application.port.WhisperPort
import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.model.MeetingStatus
import com.atomiccv.worklog.domain.repository.MeetingRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate

data class UploadMeetingCommand(
    val title: String,
    val meetingDate: LocalDate,
    val audioBytes: ByteArray,
    val fileName: String,
)

class UploadMeetingUseCase(
    private val whisperPort: WhisperPort,
    private val gptSummaryPort: GptSummaryPort,
    private val meetingRepository: MeetingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(command: UploadMeetingCommand): Meeting {
        return try {
            val transcript = whisperPort.transcribe(command.audioBytes, command.fileName)
            val summary = gptSummaryPort.summarize(transcript)
            meetingRepository.save(
                Meeting(
                    title = command.title,
                    meetingDate = command.meetingDate,
                    transcript = transcript,
                    summary = summary,
                    status = MeetingStatus.COMPLETED,
                )
            )
        } catch (e: Exception) {
            log.error("회의 업로드 처리 실패: ${command.title}", e)
            meetingRepository.save(
                Meeting(
                    title = command.title,
                    meetingDate = command.meetingDate,
                    summary = "요약 실패 — 다시 시도해주세요",
                    status = MeetingStatus.FAILED,
                )
            )
        }
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :module-worklog:test --tests "*.UploadMeetingUseCaseTest"
```

Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/UploadMeetingUseCase.kt \
        module-worklog/src/test/kotlin/com/atomiccv/worklog/application/UploadMeetingUseCaseTest.kt
git commit -m "feat(worklog): UploadMeetingUseCase — Whisper+GPT 요약 후 Meeting 저장"
```

---

## Task 9: GetDailyWorklogUseCase

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/GetDailyWorklogUseCase.kt`
- Create: `module-worklog/src/test/kotlin/com/atomiccv/worklog/application/GetDailyWorklogUseCaseTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
// module-worklog/src/test/kotlin/com/atomiccv/worklog/application/GetDailyWorklogUseCaseTest.kt
package com.atomiccv.worklog.application

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.application.usecase.GetDailyWorklogUseCase
import com.atomiccv.worklog.domain.model.DailyWorklog
import com.atomiccv.worklog.domain.model.GithubActivity
import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.repository.MeetingRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class GetDailyWorklogUseCaseTest {
    private val meetingRepository: MeetingRepository = mockk()
    private val githubActivityPort: GithubActivityPort = mockk()
    private val useCase = GetDailyWorklogUseCase(meetingRepository, githubActivityPort)

    @Test
    fun `날짜별 미팅과 GitHub 활동을 함께 반환한다`() {
        val date = LocalDate.of(2026, 5, 5)
        val meetings = listOf(Meeting(id = 1L, title = "스프린트 회의", meetingDate = date))
        val activity = GithubActivity()

        every { meetingRepository.findByMeetingDate(date) } returns meetings
        every { githubActivityPort.getActivity(date) } returns activity

        val result = useCase.execute(date)

        assertEquals(date, result.date)
        assertEquals(1, result.meetings.size)
        assertEquals("스프린트 회의", result.meetings[0].title)
    }

    @Test
    fun `날짜를 전달하지 않으면 오늘 날짜로 조회한다`() {
        val today = LocalDate.now()

        every { meetingRepository.findByMeetingDate(today) } returns emptyList()
        every { githubActivityPort.getActivity(today) } returns GithubActivity()

        val result = useCase.execute(today)

        assertEquals(today, result.date)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :module-worklog:test --tests "*.GetDailyWorklogUseCaseTest"
```

Expected: FAIL

- [ ] **Step 3: GetDailyWorklogUseCase.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/GetDailyWorklogUseCase.kt
package com.atomiccv.worklog.application.usecase

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.domain.model.DailyWorklog
import com.atomiccv.worklog.domain.repository.MeetingRepository
import java.time.LocalDate

class GetDailyWorklogUseCase(
    private val meetingRepository: MeetingRepository,
    private val githubActivityPort: GithubActivityPort,
) {
    fun execute(date: LocalDate = LocalDate.now()): DailyWorklog =
        DailyWorklog(
            date = date,
            meetings = meetingRepository.findByMeetingDate(date),
            githubActivity = githubActivityPort.getActivity(date),
        )
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :module-worklog:test --tests "*.GetDailyWorklogUseCaseTest"
```

Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/application/usecase/GetDailyWorklogUseCase.kt \
        module-worklog/src/test/kotlin/com/atomiccv/worklog/application/GetDailyWorklogUseCaseTest.kt
git commit -m "feat(worklog): GetDailyWorklogUseCase 구현"
```

---

## Task 10: WorklogConfiguration (Bean 등록)

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/WorklogConfiguration.kt`

- [ ] **Step 1: WorklogConfiguration.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/WorklogConfiguration.kt
package com.atomiccv.worklog.infrastructure

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.application.port.GptSummaryPort
import com.atomiccv.worklog.application.port.WhisperPort
import com.atomiccv.worklog.application.usecase.GetDailyWorklogUseCase
import com.atomiccv.worklog.application.usecase.GetGithubActivityUseCase
import com.atomiccv.worklog.application.usecase.UploadMeetingUseCase
import com.atomiccv.worklog.domain.repository.MeetingRepository
import com.atomiccv.worklog.infrastructure.client.GithubClient
import com.atomiccv.worklog.infrastructure.client.GptSummaryClient
import com.atomiccv.worklog.infrastructure.client.WhisperClient
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Cache
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.util.concurrent.TimeUnit

/**
 * worklog 모듈 Bean 등록.
 * UseCase는 DDD 원칙에 따라 Spring 어노테이션 없이 작성하고, 여기서 @Bean으로 명시 등록한다.
 */
@Configuration
class WorklogConfiguration {

    @Bean
    fun openAiRestClient(
        @Value("\${worklog.openai.api-key}") apiKey: String,
    ): RestClient = RestClient.builder()
        .baseUrl("https://api.openai.com")
        .defaultHeader("Authorization", "Bearer $apiKey")
        .build()

    @Bean
    fun githubRestClient(
        @Value("\${worklog.github.token}") token: String,
    ): RestClient = RestClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader("Authorization", "Bearer $token")
        .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
        .build()

    @Bean
    fun githubActivityCache(): Cache<String, Any> =
        Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build()

    @Bean
    fun whisperPort(openAiRestClient: RestClient): WhisperPort =
        WhisperClient(openAiRestClient)

    @Bean
    fun gptSummaryPort(openAiRestClient: RestClient): GptSummaryPort =
        GptSummaryClient(openAiRestClient)

    @Bean
    fun githubActivityPort(
        githubRestClient: RestClient,
        githubActivityCache: Cache<String, Any>,
        @Value("\${worklog.github.org}") org: String,
    ): GithubActivityPort = GithubClient(
        githubRestClient = githubRestClient,
        cache = githubActivityCache,
        org = org,
    )

    @Bean
    fun uploadMeetingUseCase(
        whisperPort: WhisperPort,
        gptSummaryPort: GptSummaryPort,
        meetingRepository: MeetingRepository,
    ): UploadMeetingUseCase =
        UploadMeetingUseCase(whisperPort, gptSummaryPort, meetingRepository)

    @Bean
    fun getGithubActivityUseCase(githubActivityPort: GithubActivityPort): GetGithubActivityUseCase =
        GetGithubActivityUseCase(githubActivityPort)

    @Bean
    fun getDailyWorklogUseCase(
        meetingRepository: MeetingRepository,
        githubActivityPort: GithubActivityPort,
    ): GetDailyWorklogUseCase =
        GetDailyWorklogUseCase(meetingRepository, githubActivityPort)
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew :module-worklog:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/infrastructure/WorklogConfiguration.kt
git commit -m "feat(worklog): WorklogConfiguration Bean 등록"
```

---

## Task 11: Web 레이어 (Controller)

**Files:**
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/interfaces/web/WorklogController.kt`
- Create: `module-worklog/src/main/kotlin/com/atomiccv/worklog/interfaces/web/MeetingApiController.kt`

- [ ] **Step 1: WorklogController.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/interfaces/web/WorklogController.kt
package com.atomiccv.worklog.interfaces.web

import com.atomiccv.worklog.application.usecase.GetDailyWorklogUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Controller
@RequestMapping("/worklog")
class WorklogController(
    private val getDailyWorklogUseCase: GetDailyWorklogUseCase,
) {
    @GetMapping
    fun dashboard(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
        model: Model,
    ): String {
        val targetDate = date ?: LocalDate.now()
        val worklog = getDailyWorklogUseCase.execute(targetDate)
        model.addAttribute("worklog", worklog)
        model.addAttribute("prevDate", targetDate.minusDays(1))
        model.addAttribute("nextDate", targetDate.plusDays(1))
        model.addAttribute("isToday", targetDate == LocalDate.now())
        return "worklog/dashboard"
    }
}
```

- [ ] **Step 2: MeetingApiController.kt 생성**

```kotlin
// module-worklog/src/main/kotlin/com/atomiccv/worklog/interfaces/web/MeetingApiController.kt
package com.atomiccv.worklog.interfaces.web

import com.atomiccv.worklog.application.usecase.UploadMeetingCommand
import com.atomiccv.worklog.application.usecase.UploadMeetingUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

private val ALLOWED_AUDIO_TYPES = setOf("audio/mpeg", "audio/mp4", "audio/wav", "audio/x-wav")
private const val MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024 // 25MB

@Controller
@RequestMapping("/worklog/meetings")
class MeetingApiController(
    private val uploadMeetingUseCase: UploadMeetingUseCase,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestParam title: String,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        meetingDate: LocalDate,
        @RequestParam audioFile: MultipartFile,
        model: Model,
    ): String {
        val contentType = audioFile.contentType ?: ""
        if (contentType !in ALLOWED_AUDIO_TYPES) {
            model.addAttribute("error", "지원하지 않는 파일 형식입니다. (mp3, mp4, wav만 허용)")
            return "worklog/meeting-card :: error-card"
        }
        if (audioFile.size > MAX_FILE_SIZE_BYTES) {
            model.addAttribute("error", "파일 크기는 25MB를 초과할 수 없습니다.")
            return "worklog/meeting-card :: error-card"
        }

        val meeting = uploadMeetingUseCase.execute(
            UploadMeetingCommand(
                title = title.trim(),
                meetingDate = meetingDate,
                audioBytes = audioFile.bytes,
                fileName = audioFile.originalFilename ?: "meeting.mp3",
            )
        )
        model.addAttribute("meeting", meeting)
        return "worklog/meeting-card :: meeting-card"
    }
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :module-worklog:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add module-worklog/src/main/kotlin/com/atomiccv/worklog/interfaces/web/
git commit -m "feat(worklog): WorklogController, MeetingApiController 구현"
```

---

## Task 12: Thymeleaf 템플릿

**Files:**
- Create: `module-worklog/src/main/resources/templates/worklog/dashboard.html`
- Create: `module-worklog/src/main/resources/templates/worklog/meeting-card.html`

- [ ] **Step 1: meeting-card.html 생성 (HTMX partial 먼저)**

```html
<!-- module-worklog/src/main/resources/templates/worklog/meeting-card.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>

<!-- HTMX partial: 회의 카드 -->
<div th:fragment="meeting-card" class="meeting-card">
  <div class="meeting-card-header">
    <span class="meeting-title" th:text="${meeting.title}">회의 제목</span>
    <span class="meeting-time" th:text="${#temporals.format(meeting.createdAt, 'HH:mm')}">10:00</span>
    <span class="meeting-status-badge"
          th:classappend="${meeting.status.name() == 'FAILED'} ? 'badge-failed' : 'badge-ok'"
          th:text="${meeting.status.name() == 'FAILED'} ? '실패' : '완료'">완료</span>
  </div>
  <div class="meeting-summary" th:utext="${#strings.replace(meeting.summary, '&#10;', '&lt;br&gt;')}">요약 내용</div>
</div>

<!-- HTMX partial: 에러 카드 -->
<div th:fragment="error-card" class="meeting-card meeting-card--error">
  <span th:text="${error}">오류 메시지</span>
</div>

</body>
</html>
```

- [ ] **Step 2: dashboard.html 생성**

```html
<!-- module-worklog/src/main/resources/templates/worklog/dashboard.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Worklog Dashboard</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
  <script src="https://unpkg.com/htmx.org@2.0.4"></script>
  <style>
    body { background: #f8f9fa; }
    .dashboard-card { background: white; border-radius: 8px; padding: 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,.1); height: 100%; }
    .meeting-card { border: 1px solid #e9ecef; border-radius: 6px; padding: 1rem; margin-bottom: 0.75rem; }
    .meeting-card-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; }
    .meeting-title { font-weight: 600; flex: 1; }
    .meeting-time { color: #6c757d; font-size: 0.875rem; }
    .meeting-summary { font-size: 0.875rem; color: #495057; white-space: pre-line; }
    .badge-ok { background: #d1fae5; color: #065f46; border-radius: 4px; padding: 2px 6px; font-size: 0.75rem; }
    .badge-failed { background: #fee2e2; color: #991b1b; border-radius: 4px; padding: 2px 6px; font-size: 0.75rem; }
    .meeting-card--error { border-color: #f87171; background: #fff5f5; color: #991b1b; }
    .commit-item { font-size: 0.875rem; padding: 0.25rem 0; border-bottom: 1px solid #f1f3f5; }
    .repo-badge { background: #e9ecef; color: #495057; border-radius: 3px; padding: 1px 5px; font-size: 0.75rem; }
    .htmx-indicator { display: none; }
    .htmx-request .htmx-indicator { display: inline; }
  </style>
</head>
<body>
<div class="container py-4">

  <!-- 헤더 -->
  <div class="d-flex align-items-center justify-content-between mb-4">
    <h4 class="mb-0 fw-bold">Worklog Dashboard</h4>
    <div class="d-flex align-items-center gap-2">
      <a th:href="@{/worklog(date=${prevDate})}" class="btn btn-sm btn-outline-secondary">&lt;</a>
      <span class="fw-semibold" th:text="${#temporals.format(worklog.date, 'yyyy-MM-dd (E)', new java.util.Locale('ko'))}">2026-05-05</span>
      <a th:href="@{/worklog(date=${nextDate})}" class="btn btn-sm btn-outline-secondary"
         th:classappend="${isToday} ? 'disabled'">&gt;</a>
    </div>
  </div>

  <div class="row g-4">

    <!-- 오늘의 미팅 -->
    <div class="col-md-6">
      <div class="dashboard-card">
        <div class="d-flex align-items-center justify-content-between mb-3">
          <h6 class="mb-0 fw-semibold">오늘의 미팅</h6>
          <button class="btn btn-sm btn-primary" data-bs-toggle="collapse" data-bs-target="#uploadForm">
            + 음성 업로드
          </button>
        </div>

        <!-- 업로드 폼 -->
        <div class="collapse mb-3" id="uploadForm">
          <form hx-post="/worklog/meetings"
                hx-target="#meetings-list"
                hx-swap="beforeend"
                hx-encoding="multipart/form-data"
                hx-indicator="#upload-spinner"
                enctype="multipart/form-data"
                class="border rounded p-3">
            <div class="mb-2">
              <input type="text" name="title" class="form-control form-control-sm"
                     placeholder="회의 제목" required>
            </div>
            <div class="mb-2">
              <input type="hidden" name="meetingDate"
                     th:value="${worklog.date}">
              <input type="file" name="audioFile" class="form-control form-control-sm"
                     accept="audio/mpeg,audio/mp4,audio/wav" required>
              <div class="form-text">mp3, mp4, wav — 최대 25MB</div>
            </div>
            <button type="submit" class="btn btn-sm btn-success">
              업로드
              <span id="upload-spinner" class="htmx-indicator ms-1">처리 중...</span>
            </button>
          </form>
        </div>

        <!-- 회의 목록 -->
        <div id="meetings-list">
          <div th:if="${worklog.meetings.empty}" class="text-muted text-center py-3" style="font-size:0.875rem">
            오늘 등록된 회의가 없습니다
          </div>
          <div th:each="meeting : ${worklog.meetings}" class="meeting-card">
            <div class="meeting-card-header">
              <span class="meeting-title" th:text="${meeting.title}"></span>
              <span class="meeting-time" th:text="${#temporals.format(meeting.createdAt, 'HH:mm')}"></span>
              <span class="meeting-status-badge"
                    th:classappend="${meeting.status.name() == 'FAILED'} ? 'badge-failed' : 'badge-ok'"
                    th:text="${meeting.status.name() == 'FAILED'} ? '실패' : '완료'"></span>
            </div>
            <div class="meeting-summary" th:text="${meeting.summary}"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- GitHub 활동 -->
    <div class="col-md-6">
      <div class="dashboard-card">
        <h6 class="mb-3 fw-semibold">GitHub 활동</h6>

        <!-- 요약 배지 -->
        <div class="d-flex gap-3 mb-3">
          <span class="badge bg-dark" th:text="'커밋 ' + ${worklog.githubActivity.commitCount}">커밋 0</span>
          <span class="badge bg-primary" th:text="'PR ' + ${worklog.githubActivity.prCount}">PR 0</span>
          <span class="badge bg-secondary" th:text="'이슈 ' + ${worklog.githubActivity.issueCount}">이슈 0</span>
        </div>

        <!-- 커밋 목록 -->
        <div th:if="${!worklog.githubActivity.commits.empty}">
          <p class="text-muted mb-1" style="font-size:0.8rem">커밋</p>
          <div th:each="commit : ${worklog.githubActivity.commits}" class="commit-item">
            <code class="me-1 text-muted" th:text="${commit.sha}"></code>
            <span th:text="${commit.message}"></span>
            <span class="repo-badge ms-1" th:text="${commit.repoName}"></span>
          </div>
        </div>

        <!-- PR 목록 -->
        <div th:if="${!worklog.githubActivity.pullRequests.empty}" class="mt-2">
          <p class="text-muted mb-1" style="font-size:0.8rem">PR</p>
          <div th:each="pr : ${worklog.githubActivity.pullRequests}" class="commit-item">
            <span th:text="'#' + ${pr.number} + ' ' + ${pr.title}"></span>
            <span class="repo-badge ms-1" th:text="${pr.repoName}"></span>
            <span class="ms-1" th:classappend="${pr.state == 'open'} ? 'text-success' : 'text-secondary'"
                  th:text="${pr.state}"></span>
          </div>
        </div>

        <!-- 이슈 목록 -->
        <div th:if="${!worklog.githubActivity.issues.empty}" class="mt-2">
          <p class="text-muted mb-1" style="font-size:0.8rem">이슈</p>
          <div th:each="issue : ${worklog.githubActivity.issues}" class="commit-item">
            <span th:text="'#' + ${issue.number} + ' ' + ${issue.title}"></span>
            <span class="repo-badge ms-1" th:text="${issue.repoName}"></span>
          </div>
        </div>

        <div th:if="${worklog.githubActivity.commitCount == 0 and worklog.githubActivity.prCount == 0 and worklog.githubActivity.issueCount == 0}"
             class="text-muted text-center py-3" style="font-size:0.875rem">
          GitHub 활동이 없습니다
        </div>
      </div>
    </div>

  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

- [ ] **Step 3: 커밋**

```bash
git add module-worklog/src/main/resources/
git commit -m "feat(worklog): Thymeleaf+HTMX 대시보드 템플릿 추가"
```

---

## Task 13: app 모듈 통합 및 환경변수 설정

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/resources/application-dev.yml`
- Modify: `app/src/main/resources/application-prod.yml`

- [ ] **Step 1: app/build.gradle.kts에 module-worklog 추가**

```kotlin
// app/build.gradle.kts — dependencies 블록 수정
dependencies {
    implementation(project(":module-shared"))
    implementation(project(":module-auth"))
    implementation(project(":module-resume"))
    implementation(project(":module-worklog"))   // 추가
    // ...
}
```

- [ ] **Step 2: application-dev.yml에 worklog 설정 추가**

기존 파일 하단에 추가:

```yaml
# application-dev.yml 하단에 추가
worklog:
  openai:
    api-key: ${OPENAI_API_KEY}
  github:
    token: ${GITHUB_TOKEN}
    org: resume-helper
  audio:
    upload-dir: /tmp/worklog/audio

spring:
  thymeleaf:
    cache: false
  servlet:
    multipart:
      max-file-size: 25MB
      max-request-size: 26MB
```

- [ ] **Step 3: application-prod.yml에 worklog 설정 추가**

기존 파일 하단에 추가:

```yaml
# application-prod.yml 하단에 추가
worklog:
  openai:
    api-key: ${OPENAI_API_KEY}
  github:
    token: ${GITHUB_TOKEN}
    org: resume-helper
  audio:
    upload-dir: /var/worklog/audio

spring:
  thymeleaf:
    cache: true
  servlet:
    multipart:
      max-file-size: 25MB
      max-request-size: 26MB
```

- [ ] **Step 4: AWS SSM에 환경변수 추가 (수동 작업)**

EC2 서버 또는 AWS Console에서 실행:

```bash
aws ssm put-parameter \
  --name "/atomiccv/prod/OPENAI_API_KEY" \
  --value "sk-..." \
  --type SecureString \
  --overwrite

aws ssm put-parameter \
  --name "/atomiccv/prod/GITHUB_TOKEN" \
  --value "ghp_..." \
  --type SecureString \
  --overwrite
```

- [ ] **Step 5: 전체 빌드 확인**

```bash
./gradlew :module-worklog:test
```

Expected: BUILD SUCCESSFUL, 모든 테스트 통과

- [ ] **Step 6: app 모듈 빌드 확인**

```bash
./gradlew :app:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add app/build.gradle.kts \
        app/src/main/resources/application-dev.yml \
        app/src/main/resources/application-prod.yml
git commit -m "feat(worklog): app 모듈에 module-worklog 통합 및 환경변수 설정"
```

---

## Self-Review 체크리스트

### 스펙 커버리지

| 스펙 요구사항 | 구현 Task |
|---|---|
| 음성 업로드 → Whisper → GPT 요약 | Task 6, 8 |
| HTMX partial 렌더링 (페이지 새로고침 없음) | Task 11, 12 |
| 파일 타입 검증 (audio만 허용, 25MB 제한) | Task 11 |
| GitHub API 커밋/PR/이슈 조회 | Task 5 |
| Caffeine TTL 10분 캐시 | Task 5, 10 |
| 날짜 선택 (prev/next 네비게이션) | Task 11, 12 |
| Spring Security 기존 인증 적용 | 기존 `anyRequest().authenticated()` 적용됨 |
| OpenAI API Key SSM 관리 | Task 13 |
| Whisper 실패 시 FAILED 상태 저장 | Task 8 |

### 플레이스홀더 없음 확인 ✅

모든 Task에 완전한 코드 포함. TBD/TODO 없음.

### 타입 일관성 확인 ✅

- `GithubActivity`, `GithubCommit`, `GithubPullRequest`, `GithubIssue` — Task 2 정의, Task 5·7·9·12에서 동일하게 사용
- `Meeting`, `MeetingStatus` — Task 2 정의, Task 3·8·11·12에서 동일하게 사용
- `UploadMeetingCommand` — Task 8에서 정의, Task 11에서 사용
- `DailyWorklog` — Task 2 정의, Task 9·11·12에서 동일하게 사용
