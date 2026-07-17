package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.repository.ResumeDetail
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class GetPublicResumeQuery(
    val slug: String,
)

/**
 * 공개 웹 이력서를 slug 로 조회한다 (비로그인 외부 열람자용).
 *
 * 비공개·삭제·부재 모두 RESUME_NOT_FOUND 로 응답해
 * 비공개 이력서의 존재 여부를 외부에 노출하지 않는다.
 */
class GetPublicResumeUseCase(
    private val resumeRepository: ResumeRepository,
) {
    fun getDetail(query: GetPublicResumeQuery): ResumeDetail {
        val resume = resumeRepository.findBySlug(query.slug)
        if (resume == null || !resume.isPublic) throw BusinessException(ErrorCode.RESUME_NOT_FOUND)
        return resumeRepository.findDetailById(resume.id)
            ?: throw BusinessException(ErrorCode.RESUME_NOT_FOUND)
    }
}
