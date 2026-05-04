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
