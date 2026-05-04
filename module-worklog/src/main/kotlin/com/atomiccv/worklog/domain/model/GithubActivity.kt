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
