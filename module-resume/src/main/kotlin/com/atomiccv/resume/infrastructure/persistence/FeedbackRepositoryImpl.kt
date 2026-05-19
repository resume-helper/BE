package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.repository.FeedbackRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class FeedbackRepositoryImpl(
    private val feedbackJpaRepository: FeedbackJpaRepository,
    private val feedbackTagJpaRepository: FeedbackTagJpaRepository,
) : FeedbackRepository {
    override fun save(feedback: Feedback): Feedback {
        val saved = feedbackJpaRepository.save(FeedbackJpaEntity.fromDomain(feedback))
        val tags =
            feedback.tags.map { tag ->
                feedbackTagJpaRepository.save(FeedbackTagJpaEntity(feedbackId = saved.id, tag = tag))
            }
        return saved.toDomain(tags.map { it.tag })
    }

    override fun findById(id: Long): Feedback? {
        val entity = feedbackJpaRepository.findById(id).orElse(null) ?: return null
        val tags = feedbackTagJpaRepository.findAllByFeedbackId(entity.id).map { it.tag }
        return entity.toDomain(tags)
    }

    override fun findAllByResumeId(
        resumeId: Long,
        page: Int,
        size: Int,
    ): List<Feedback> {
        val pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending())
        val entities = feedbackJpaRepository.findAllByResumeId(resumeId, pageable).content
        if (entities.isEmpty()) return emptyList()
        val tagsMap =
            feedbackTagJpaRepository
                .findAllByFeedbackIdIn(entities.map { it.id })
                .groupBy { it.feedbackId }
        return entities.map { entity ->
            entity.toDomain(tagsMap[entity.id]?.map { it.tag } ?: emptyList())
        }
    }

    override fun countByResumeId(resumeId: Long): Long = feedbackJpaRepository.countByResumeId(resumeId)

    override fun findAllByResumeIdIn(
        resumeIds: List<Long>,
        page: Int,
        size: Int,
    ): List<Feedback> {
        if (resumeIds.isEmpty()) return emptyList()
        val pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending())
        val entities = feedbackJpaRepository.findAllByResumeIdIn(resumeIds, pageable).content
        val tagsMap =
            feedbackTagJpaRepository
                .findAllByFeedbackIdIn(entities.map { it.id })
                .groupBy { it.feedbackId }
        return entities.map { entity ->
            entity.toDomain(tagsMap[entity.id]?.map { it.tag } ?: emptyList())
        }
    }

    override fun countByResumeIdIn(resumeIds: List<Long>): Long {
        if (resumeIds.isEmpty()) return 0L
        return feedbackJpaRepository.countByResumeIdIn(resumeIds)
    }

    override fun deleteById(feedbackId: Long) {
        feedbackTagJpaRepository.deleteAllByFeedbackId(feedbackId)
        feedbackJpaRepository.deleteById(feedbackId)
    }
}
