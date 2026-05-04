package com.atomiccv.worklog.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MeetingJpaRepository : JpaRepository<MeetingJpaEntity, Long> {
    fun findByMeetingDate(meetingDate: LocalDate): List<MeetingJpaEntity>
}
