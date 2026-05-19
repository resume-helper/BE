package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.ResumeBlock

interface ResumeBlockRepository {
    fun findAllByResumeId(resumeId: Long): List<ResumeBlock>

    fun deleteAllByResumeId(resumeId: Long)

    fun saveAll(resumeBlocks: List<ResumeBlock>): List<ResumeBlock>
}
