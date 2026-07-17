package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.ViewSession
import com.atomiccv.resume.domain.repository.SectionDwellAverage
import com.atomiccv.resume.domain.repository.ViewAggregate
import com.atomiccv.resume.domain.repository.ViewSessionRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ViewSessionRepositoryImpl(
    private val viewSessionJpaRepository: ViewSessionJpaRepository,
    private val sectionDwellJpaRepository: SectionDwellJpaRepository,
) : ViewSessionRepository {
    override fun save(viewSession: ViewSession): ViewSession {
        val saved = viewSessionJpaRepository.save(ViewSessionJpaEntity.fromDomain(viewSession))
        if (viewSession.id != 0L) sectionDwellJpaRepository.deleteAllByViewSessionId(saved.id)
        val dwells =
            viewSession.sectionDwells.map {
                sectionDwellJpaRepository.save(
                    SectionDwellJpaEntity(
                        viewSessionId = saved.id,
                        sectionName = it.section.name,
                        dwellSeconds = it.dwellSeconds,
                    ),
                )
            }
        return saved.toDomain(dwells.map { it.toDomain() })
    }

    override fun findById(id: Long): ViewSession? {
        val entity = viewSessionJpaRepository.findById(id).orElse(null) ?: return null
        val dwells = sectionDwellJpaRepository.findAllByViewSessionId(entity.id).map { it.toDomain() }
        return entity.toDomain(dwells)
    }

    override fun countByResumeId(resumeId: Long): Long = viewSessionJpaRepository.countByResumeId(resumeId)

    override fun countDistinctVisitorsByResumeId(resumeId: Long): Long =
        viewSessionJpaRepository.countDistinctVisitorsByResumeId(resumeId)

    override fun aggregateByResumeIdBetween(
        resumeId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): ViewAggregate {
        val row = viewSessionJpaRepository.aggregateByResumeIdBetween(resumeId, from, to).first()
        return ViewAggregate(
            viewCount = (row[0] as Number).toLong(),
            averageDurationSec = (row[1] as Number?)?.toDouble(),
        )
    }

    override fun averageSectionDwellsByResumeId(resumeId: Long): List<SectionDwellAverage> =
        sectionDwellJpaRepository.averageDwellsByResumeId(resumeId).map { row ->
            SectionDwellAverage(
                section = BlockType.valueOf(row[0] as String),
                averageDwellSec = (row[1] as Number).toDouble(),
            )
        }
}
