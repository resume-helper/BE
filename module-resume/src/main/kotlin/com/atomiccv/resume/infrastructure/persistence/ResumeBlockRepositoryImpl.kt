package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.ResumeBlock
import com.atomiccv.resume.domain.repository.ResumeBlockRepository
import org.springframework.stereotype.Repository

@Repository
class ResumeBlockRepositoryImpl(
    private val jpaRepository: ResumeBlockJpaRepository,
) : ResumeBlockRepository {
    override fun findAllByResumeId(resumeId: Long): List<ResumeBlock> =
        jpaRepository.findAllByResumeId(resumeId).map { it.toDomain() }

    override fun deleteAllByResumeId(resumeId: Long) = jpaRepository.deleteAllByResumeId(resumeId)

    override fun saveAll(resumeBlocks: List<ResumeBlock>): List<ResumeBlock> =
        jpaRepository.saveAll(resumeBlocks.map { ResumeBlockJpaEntity.fromDomain(it) }).map { it.toDomain() }
}
