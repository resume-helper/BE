package com.atomiccv.worklog.application.port

import com.atomiccv.worklog.domain.model.GithubActivity
import java.time.LocalDate

interface GithubActivityPort {
    fun getActivity(date: LocalDate): GithubActivity
}
