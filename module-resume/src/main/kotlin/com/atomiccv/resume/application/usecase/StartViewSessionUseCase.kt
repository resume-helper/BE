package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.ViewSession
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.ViewSessionRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class StartViewSessionCommand(
    val slug: String,
    val visitorIp: String,
)

/**
 * 공개 웹 이력서 열람 시작 — 열람 세션 1개 생성 (조회수 1 집계).
 * 비공개·삭제·부재는 공개 조회와 동일하게 RESUME_NOT_FOUND 로 응답한다.
 */
class StartViewSessionUseCase(
    private val resumeRepository: ResumeRepository,
    private val viewSessionRepository: ViewSessionRepository,
) {
    fun start(command: StartViewSessionCommand): ViewSession {
        val resume = resumeRepository.findBySlug(command.slug)
        if (resume == null || !resume.isPublic) throw BusinessException(ErrorCode.RESUME_NOT_FOUND)
        return viewSessionRepository.save(
            ViewSession(resumeId = resume.id, visitorIp = command.visitorIp),
        )
    }
}
