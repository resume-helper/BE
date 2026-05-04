package com.atomiccv.worklog.infrastructure.persistence

import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.repository.MeetingRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MeetingRepositoryImpl(
    private val jpaRepository: MeetingJpaRepository,
) : MeetingRepository {
    override fun save(meeting: Meeting): Meeting = jpaRepository.save(MeetingJpaEntity.fromDomain(meeting)).toDomain()

    override fun update(meeting: Meeting): Meeting {
        // 기존 엔티티의 createdAt을 유지한 채 업데이트
        val existing =
            jpaRepository.findById(meeting.id).orElseThrow {
                IllegalArgumentException("Meeting not found: ${meeting.id}")
            }
        return jpaRepository
            .save(
                MeetingJpaEntity.fromDomainWithCreatedAt(meeting, existing.createdAt)
            ).toDomain()
    }

    override fun findById(id: Long): Meeting? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByMeetingDate(date: LocalDate): List<Meeting> =
        jpaRepository.findByMeetingDate(date).map { it.toDomain() }
}
