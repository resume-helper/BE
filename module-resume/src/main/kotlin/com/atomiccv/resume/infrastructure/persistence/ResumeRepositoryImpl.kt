package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeRepository
import org.springframework.stereotype.Repository

@Repository
class ResumeRepositoryImpl(
    private val jpaRepository: ResumeJpaRepository,
) : ResumeRepository {
    override fun save(resume: Resume): Resume = jpaRepository.save(ResumeJpaEntity.fromDomain(resume)).toDomain()

    override fun findById(id: Long): Resume? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findBySlug(slug: String): Resume? = jpaRepository.findBySlug(slug)?.toDomain()

    override fun findAllByUserId(userId: Long): List<Resume> =
        jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId).map { it.toDomain() }

    override fun deleteById(id: Long) = jpaRepository.deleteById(id)
}
