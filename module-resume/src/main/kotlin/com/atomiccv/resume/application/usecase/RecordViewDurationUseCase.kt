package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.SectionDwell
import com.atomiccv.resume.domain.model.ViewSession
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.ViewSessionRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional

data class RecordViewDurationCommand(
    val slug: String,
    val sessionId: Long,
    val totalDurationSec: Int,
    val sectionDwells: List<SectionDwell>,
)

/**
 * 열람 세션의 체류시간 보고 (이탈 시 1회 또는 재보고 — 멱등 갱신).
 */
@Transactional
class RecordViewDurationUseCase(
    private val resumeRepository: ResumeRepository,
    private val viewSessionRepository: ViewSessionRepository,
) {
    fun record(command: RecordViewDurationCommand): ViewSession {
        validateDurations(command)
        val session = findSessionOfSlug(command)
        return viewSessionRepository.save(
            session.copy(
                totalDurationSec = command.totalDurationSec,
                sectionDwells = command.sectionDwells,
            ),
        )
    }

    private fun validateDurations(command: RecordViewDurationCommand) {
        val hasNegative =
            command.totalDurationSec < 0 || command.sectionDwells.any { it.dwellSeconds < 0 }
        if (hasNegative) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED, "체류시간은 0 이상이어야 합니다.")
        }
    }

    private fun findSessionOfSlug(command: RecordViewDurationCommand): ViewSession {
        val resume = resumeRepository.findBySlug(command.slug)
        if (resume == null || !resume.isPublic) throw BusinessException(ErrorCode.RESUME_NOT_FOUND)
        val session = viewSessionRepository.findById(command.sessionId)
        if (session == null || session.resumeId != resume.id) {
            throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "열람 세션을 찾을 수 없습니다.")
        }
        return session
    }
}
