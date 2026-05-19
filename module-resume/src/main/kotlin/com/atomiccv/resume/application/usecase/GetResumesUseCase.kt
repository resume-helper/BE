package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.ResumeListQuery
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.SortDirection
import org.springframework.data.domain.Page

data class GetResumesQuery(
    val userId: Long,
    val type: ResumeType?,
    val titleKeyword: String?,
    val page: Int,
    val size: Int,
    val sortDirection: SortDirection,
)

class GetResumesUseCase(
    private val resumeRepository: ResumeRepository,
) {
    fun getList(query: GetResumesQuery): Page<Resume> =
        resumeRepository.findPageByUserId(
            userId = query.userId,
            query =
                ResumeListQuery(
                    userId = query.userId,
                    type = query.type,
                    titleKeyword = query.titleKeyword,
                    page = query.page - 1,
                    size = query.size,
                    sortDirection = query.sortDirection,
                ),
        )
}
