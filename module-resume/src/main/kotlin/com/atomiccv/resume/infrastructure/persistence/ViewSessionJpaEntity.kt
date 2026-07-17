package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.SectionDwell
import com.atomiccv.resume.domain.model.ViewSession
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "view_sessions")
@EntityListeners(AuditingEntityListener::class)
class ViewSessionJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "resume_id", nullable = false)
    val resumeId: Long,
    @Column(name = "visitor_ip", nullable = false, length = 45)
    val visitorIp: String,
    // null = 체류시간 미보고 세션 (평균 집계에서 제외)
    @Column(name = "total_duration_sec")
    val totalDurationSec: Int? = null,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain(sectionDwells: List<SectionDwell>): ViewSession =
        ViewSession(
            id = id,
            resumeId = resumeId,
            visitorIp = visitorIp,
            totalDurationSec = totalDurationSec,
            sectionDwells = sectionDwells,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(viewSession: ViewSession): ViewSessionJpaEntity =
            ViewSessionJpaEntity(
                id = viewSession.id,
                resumeId = viewSession.resumeId,
                visitorIp = viewSession.visitorIp,
                totalDurationSec = viewSession.totalDurationSec,
            ).also { if (viewSession.id != 0L) it.createdAt = viewSession.createdAt }
    }
}

@Entity
@Table(name = "section_dwells")
@EntityListeners(AuditingEntityListener::class)
class SectionDwellJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "view_session_id", nullable = false)
    val viewSessionId: Long,
    // ERD 예약 컬럼 — 1차에서는 섹션(카테고리) 단위 집계만 사용, 블록 연결은 미사용
    @Column(name = "block_id")
    val blockId: Long? = null,
    // BlockType.name 저장 (네이티브 enum 드리프트 함정 회피를 위해 VARCHAR)
    @Column(name = "section_name", nullable = false, length = 20)
    val sectionName: String,
    @Column(name = "dwell_seconds", nullable = false)
    val dwellSeconds: Int,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain(): SectionDwell = SectionDwell(section = BlockType.valueOf(sectionName), dwellSeconds = dwellSeconds)
}
