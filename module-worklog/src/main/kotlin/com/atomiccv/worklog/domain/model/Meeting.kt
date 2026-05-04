package com.atomiccv.worklog.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Meeting(
    val id: Long = 0,
    val title: String,
    val meetingDate: LocalDate,
    val transcript: String = "",
    val summary: String = "",
    val status: MeetingStatus = MeetingStatus.COMPLETED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
