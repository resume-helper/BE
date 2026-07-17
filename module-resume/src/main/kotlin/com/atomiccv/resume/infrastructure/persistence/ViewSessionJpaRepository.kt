package com.atomiccv.resume.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ViewSessionJpaRepository : JpaRepository<ViewSessionJpaEntity, Long> {
    fun countByResumeId(resumeId: Long): Long

    @Query("select count(distinct v.visitorIp) from ViewSessionJpaEntity v where v.resumeId = :resumeId")
    fun countDistinctVisitorsByResumeId(
        @Param("resumeId") resumeId: Long,
    ): Long

    // avg 는 SQL 시맨틱대로 null(미보고 세션)을 제외하고 계산된다
    @Query(
        "select count(v), avg(v.totalDurationSec) from ViewSessionJpaEntity v " +
            "where v.resumeId = :resumeId and v.createdAt >= :from and v.createdAt < :to",
    )
    fun aggregateByResumeIdBetween(
        @Param("resumeId") resumeId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<Array<Any?>>
}

interface SectionDwellJpaRepository : JpaRepository<SectionDwellJpaEntity, Long> {
    fun findAllByViewSessionId(viewSessionId: Long): List<SectionDwellJpaEntity>

    // bulk @Modifying 에는 항상 flushAutomatically = true 동반 (module-resume/CLAUDE.md)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SectionDwellJpaEntity d where d.viewSessionId = :viewSessionId")
    fun deleteAllByViewSessionId(
        @Param("viewSessionId") viewSessionId: Long,
    )

    @Query(
        "select d.sectionName, avg(d.dwellSeconds) from SectionDwellJpaEntity d " +
            "join ViewSessionJpaEntity v on v.id = d.viewSessionId " +
            "where v.resumeId = :resumeId group by d.sectionName",
    )
    fun averageDwellsByResumeId(
        @Param("resumeId") resumeId: Long,
    ): List<Array<Any?>>
}
