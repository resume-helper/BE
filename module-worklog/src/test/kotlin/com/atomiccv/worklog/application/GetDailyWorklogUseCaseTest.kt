package com.atomiccv.worklog.application

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.application.usecase.GetDailyWorklogUseCase
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

        every { meetingRepository.findByMeetingDate(date) } returns meetings
        every { githubActivityPort.getActivity(date) } returns GithubActivity()

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
