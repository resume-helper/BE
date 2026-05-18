package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.shared.infrastructure.persistence.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "resumes")
@Suppress("LongParameterList")
class ResumeJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    val type: ResumeType? = null,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(nullable = true, length = 100, unique = true)
    val slug: String? = null,
    @Column(name = "is_public", nullable = false)
    val isPublic: Boolean = false,
    @Column(name = "pdf_s3_key", length = 500)
    val pdfS3Key: String? = null,
    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,
) : BaseJpaEntity() {
    fun toDomain() =
        Resume(
            id = id,
            userId = userId,
            type = type,
            title = title,
            slug = slug,
            isPublic = isPublic,
            pdfS3Key = pdfS3Key,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )

    companion object {
        fun fromDomain(resume: Resume) =
            ResumeJpaEntity(
                id = resume.id,
                userId = resume.userId,
                type = resume.type,
                title = resume.title,
                slug = resume.slug,
                isPublic = resume.isPublic,
                pdfS3Key = resume.pdfS3Key,
                deletedAt = resume.deletedAt,
            )
    }
}
