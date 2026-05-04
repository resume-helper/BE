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

data class GithubCommitAuthor(
    val name: String,
)

data class GithubRepo(
    val name: String,
)

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

    private fun fetchActivity(date: LocalDate) =
        try {
            val commits = fetchCommits(date)
            val (prs, issues) = fetchPrsAndIssues(date)
            GithubActivity(commits = commits, pullRequests = prs, issues = issues)
        } catch (e: Exception) {
            log.warn("GitHub API 조회 실패: ${e.message}")
            GithubActivity()
        }

    private fun fetchCommits(date: LocalDate): List<GithubCommit> {
        val response =
            githubRestClient
                .get()
                .uri("/search/commits?q=org:$org+author-date:$date&per_page=30")
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(GithubSearchCommitsResponse::class.java)
                ?: return emptyList()

        return response.items.map { item ->
            GithubCommit(
                sha = item.sha.take(7),
                message =
                    item.commit.message
                        .lines()
                        .first(),
                authorName = item.commit.author.name,
                repoName = item.repository?.name ?: "",
            )
        }
    }

    private fun fetchPrsAndIssues(date: LocalDate): Pair<List<GithubPullRequest>, List<GithubIssue>> {
        val response =
            githubRestClient
                .get()
                .uri("/search/issues?q=org:$org+updated:$date&per_page=30")
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(GithubSearchIssuesResponse::class.java)
                ?: return Pair(emptyList(), emptyList())

        val repoNameRegex = Regex("/repos/[^/]+/([^/]+)$")
        val prs =
            response.items
                .filter { it.pullRequest != null }
                .map { item ->
                    GithubPullRequest(
                        number = item.number,
                        title = item.title,
                        state = item.state,
                        repoName = repoNameRegex.find(item.repositoryUrl)?.groupValues?.get(1) ?: "",
                    )
                }
        val issues =
            response.items
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
