package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Feedback
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "feedbacks")
@EntityListeners(AuditingEntityListener::class)
class FeedbackJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "resume_id", nullable = false)
    val resumeId: Long,
    // 네이티브 enum 드리프트 함정 회피를 위해 VARCHAR 로 고정 (module-resume/CLAUDE.md 참조)
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    val section: BlockType? = null,
    // 별점 0.5 단위 — DECIMAL(2,1). 기존 TINYINT 컬럼은 ALTER 필요 (module-resume/CLAUDE.md 참조)
    @Column(columnDefinition = "DECIMAL(2,1)")
    val rating: BigDecimal?,
    @Column(columnDefinition = "TEXT")
    val comment: String?,
    @Column(name = "reviewer_ip", nullable = false, length = 45)
    val reviewerIp: String,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain(tags: List<String>): Feedback =
        Feedback(
            id = id,
            resumeId = resumeId,
            section = section,
            rating = rating?.toDouble(),
            comment = comment,
            reviewerIp = reviewerIp,
            tags = tags,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(feedback: Feedback): FeedbackJpaEntity =
            FeedbackJpaEntity(
                id = feedback.id,
                resumeId = feedback.resumeId,
                section = feedback.section,
                rating = feedback.rating?.let { BigDecimal.valueOf(it) },
                comment = feedback.comment,
                reviewerIp = feedback.reviewerIp,
            )
    }
}
