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
        val expected =
            GithubActivity(
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
