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
