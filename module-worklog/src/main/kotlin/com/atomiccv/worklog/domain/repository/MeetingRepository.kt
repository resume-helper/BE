package com.atomiccv.worklog.domain.repository

import com.atomiccv.worklog.domain.model.Meeting
import java.time.LocalDate

interface MeetingRepository {
    fun save(meeting: Meeting): Meeting

    fun update(meeting: Meeting): Meeting

    fun findById(id: Long): Meeting?

    fun findByMeetingDate(date: LocalDate): List<Meeting>
}
