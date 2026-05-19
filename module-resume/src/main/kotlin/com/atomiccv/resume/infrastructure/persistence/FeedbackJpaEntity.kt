package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Feedback
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
@Table(name = "feedbacks")
@EntityListeners(AuditingEntityListener::class)
class FeedbackJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "resume_id", nullable = false)
    val resumeId: Long,
    @Column(nullable = false)
    val rating: Byte,
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
            rating = rating.toInt(),
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
                rating = feedback.rating.toByte(),
                comment = feedback.comment,
                reviewerIp = feedback.reviewerIp,
            )
    }
}
