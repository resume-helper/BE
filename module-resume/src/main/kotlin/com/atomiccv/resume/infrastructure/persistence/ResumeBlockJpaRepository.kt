package com.atomiccv.resume.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ResumeBlockJpaRepository : JpaRepository<ResumeBlockJpaEntity, Long> {
    fun findAllByResumeId(resumeId: Long): List<ResumeBlockJpaEntity>

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ResumeBlockJpaEntity r WHERE r.resumeId = :resumeId")
    fun deleteAllByResumeId(resumeId: Long)
}
