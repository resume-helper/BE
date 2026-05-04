package com.atomiccv.worklog.application.usecase

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.domain.model.GithubActivity
import java.time.LocalDate

class GetGithubActivityUseCase(
    private val githubActivityPort: GithubActivityPort,
) {
    fun execute(date: LocalDate = LocalDate.now()): GithubActivity = githubActivityPort.getActivity(date)
}
