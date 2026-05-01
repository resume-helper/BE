package com.atomiccv.resume.application.event

data class ResumePublishedEvent(
    val resumeId: Long,
    val userId: Long,
    val slug: String,
    val versionId: Long,
)
