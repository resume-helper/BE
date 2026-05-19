package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeBlock
import org.springframework.data.domain.Page

interface ResumeRepository {
    fun save(resume: Resume): Resume

    fun findById(id: Long): Resume?

    fun findBySlug(slug: String): Resume?

    fun findAllByUserId(userId: Long): List<Resume>

    fun deleteById(id: Long)

    fun findPageByUserId(
        userId: Long,
        query: ResumeListQuery,
    ): Page<Resume>

    fun saveBlock(block: ResumeBlock): ResumeBlock

    fun findBlocksByResumeId(resumeId: Long): List<ResumeBlock>

    fun deleteBlocksByResumeId(resumeId: Long)

    fun findDetailById(resumeId: Long): ResumeDetail?
}
