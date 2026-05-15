package com.atomiccv.resume.domain.model

data class ResumeBlock(
    val id: Long = 0,
    val resumeId: Long,
    val blockId: Long,
    val orderIndex: Int,
)
