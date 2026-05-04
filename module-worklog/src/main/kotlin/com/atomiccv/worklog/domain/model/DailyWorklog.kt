package com.atomiccv.worklog.domain.model

import java.time.LocalDate

data class DailyWorklog(
    val date: LocalDate,
    val meetings: List<Meeting>,
    val githubActivity: GithubActivity,
)
