package com.atomiccv.resume.domain.repository

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType

data class ResumeListQuery(
    val userId: Long,
    val type: ResumeType?,
    val titleKeyword: String?,
    val page: Int,
    val size: Int,
    val sortDirection: SortDirection,
)

enum class SortDirection { NEWEST, OLDEST }

data class ResumeDetail(
    val resume: Resume,
    val blocks: List<ResumeBlockDetail>,
)

data class ResumeBlockDetail(
    val blockId: Long,
    val orderIndex: Int,
    val title: String,
    val type: BlockType,
    val contentJson: String,
)
