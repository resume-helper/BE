package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeBlock
import com.atomiccv.resume.domain.repository.ResumeBlockDetail
import com.atomiccv.resume.domain.repository.ResumeDetail
import com.atomiccv.resume.domain.repository.ResumeListQuery
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.SortDirection
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
@Suppress("TooManyFunctions")
class ResumeRepositoryImpl(
    private val jpaRepository: ResumeJpaRepository,
    private val blockJpaRepository: ResumeBlockJpaRepository,
) : ResumeRepository {
    override fun save(resume: Resume): Resume = jpaRepository.save(ResumeJpaEntity.fromDomain(resume)).toDomain()

    override fun findById(id: Long): Resume? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findBySlug(slug: String): Resume? = jpaRepository.findBySlugAndDeletedAtIsNull(slug)?.toDomain()

    override fun findAllByUserId(userId: Long): List<Resume> =
        jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, Pageable.unpaged()).content.map { it.toDomain() }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun findPageByUserId(
        userId: Long,
        query: ResumeListQuery,
    ): Page<Resume> {
        val pageable = buildPageable(query)
        return selectQuery(userId, query, pageable).map { it.toDomain() }
    }

    override fun saveBlock(block: ResumeBlock): ResumeBlock =
        blockJpaRepository.save(ResumeBlockJpaEntity.fromDomain(block)).toDomain()

    override fun findBlocksByResumeId(resumeId: Long): List<ResumeBlock> =
        blockJpaRepository.findAllByResumeId(resumeId).map { it.toDomain() }

    override fun deleteBlocksByResumeId(resumeId: Long) {
        blockJpaRepository.deleteAllByResumeId(resumeId)
    }

    override fun findDetailById(resumeId: Long): ResumeDetail? {
        val resume = findById(resumeId) ?: return null
        val blocks =
            jpaRepository
                .findBlockDetailsByResumeId(resumeId)
                .map { proj ->
                    ResumeBlockDetail(
                        blockId = proj.blockId,
                        orderIndex = proj.orderIndex,
                        title = proj.title,
                        type = proj.type,
                        contentJson = proj.contentJson,
                    )
                }
        return ResumeDetail(resume = resume, blocks = blocks)
    }

    private fun buildPageable(query: ResumeListQuery): PageRequest {
        val sort =
            when (query.sortDirection) {
                SortDirection.NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt")
                SortDirection.OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt")
            }
        return PageRequest.of(query.page, query.size, sort)
    }

    private fun selectQuery(
        userId: Long,
        query: ResumeListQuery,
        pageable: Pageable,
    ): Page<ResumeJpaEntity> =
        when {
            query.type != null && !query.titleKeyword.isNullOrBlank() ->
                jpaRepository.findAllByUserIdAndTypeAndTitleContainingAndDeletedAtIsNull(
                    userId,
                    query.type,
                    query.titleKeyword,
                    pageable,
                )
            query.type != null ->
                jpaRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(userId, query.type, pageable)
            !query.titleKeyword.isNullOrBlank() ->
                jpaRepository.findAllByUserIdAndTitleContainingAndDeletedAtIsNull(
                    userId,
                    query.titleKeyword,
                    pageable,
                )
            else ->
                jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)
        }
}
