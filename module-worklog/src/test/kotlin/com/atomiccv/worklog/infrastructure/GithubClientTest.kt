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
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class GithubClientTest {
    private val restClient: RestClient = mockk()
    private val cache: Cache<String, Any> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build()
    private val client =
        GithubClient(
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
        every { requestSpec.retrieve() } throws ResourceAccessException("GitHub API 오류")

        val result = client.getActivity(date)

        assertEquals(0, result.commitCount)
        assertEquals(0, result.prCount)
        assertEquals(0, result.issueCount)
    }
}
