package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.Resume

interface ResumeRepository {
    fun save(resume: Resume): Resume
    fun findById(id: Long): Resume?
    fun findBySlug(slug: String): Resume?
    fun findAllByUserId(userId: Long): List<Resume>
    fun deleteById(id: Long)
}
