package com.atomiccv.worklog.infrastructure.persistence

import com.atomiccv.shared.infrastructure.persistence.BaseJpaEntity
import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.model.MeetingStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "meetings")
class MeetingJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val title: String,
    @Column(name = "meeting_date", nullable = false)
    val meetingDate: LocalDate,
    @Column
    val transcript: String = "",
    @Column
    val summary: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: MeetingStatus = MeetingStatus.COMPLETED,
    // 업데이트 시 기존 createdAt 복원용 (null이면 JPA Auditing이 자동 설정)
    initCreatedAt: LocalDateTime? = null,
) : BaseJpaEntity() {
    init {
        if (initCreatedAt != null) {
            createdAt = initCreatedAt
        }
    }

    fun toDomain() =
        Meeting(
            id = id,
            title = title,
            meetingDate = meetingDate,
            transcript = transcript,
            summary = summary,
            status = status,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(meeting: Meeting) =
            MeetingJpaEntity(
                id = meeting.id,
                title = meeting.title,
                meetingDate = meeting.meetingDate,
                transcript = meeting.transcript,
                summary = meeting.summary,
                status = meeting.status,
            )

        fun fromDomainWithCreatedAt(
            meeting: Meeting,
            createdAt: LocalDateTime
        ) = MeetingJpaEntity(
            id = meeting.id,
            title = meeting.title,
            meetingDate = meeting.meetingDate,
            transcript = meeting.transcript,
            summary = meeting.summary,
            status = meeting.status,
            initCreatedAt = createdAt,
        )
    }
}
