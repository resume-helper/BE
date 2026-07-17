package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.model.Feedback
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.FeedbackRepository
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode

data class GetFeedbackStatsQuery(
    val resumeId: Long,
    val requestUserId: Long,
)

data class FeedbackStatsResult(
    val totalCount: Long,
    /** 이력서 전체 피드백(section = null) 통계 */
    val overall: FeedbackGroupStats,
    /** 피드백이 존재하는 섹션별 통계 */
    val sections: List<SectionFeedbackStats>,
)

data class FeedbackGroupStats(
    val count: Long,
    /** 별점이 입력된 피드백들의 평균. 별점이 하나도 없으면 null */
    val averageRating: Double?,
    val tagCounts: Map<String, Long>,
)

data class SectionFeedbackStats(
    val section: BlockType,
    val count: Long,
    val averageRating: Double?,
    val tagCounts: Map<String, Long>,
)

class GetFeedbackStatsUseCase(
    private val resumeRepository: ResumeRepository,
    private val feedbackRepository: FeedbackRepository,
) {
    fun getStats(query: GetFeedbackStatsQuery): FeedbackStatsResult {
        val resume = findResume(query.resumeId)
        if (!resume.isOwnedBy(query.requestUserId)) throw BusinessException(ErrorCode.FORBIDDEN)

        val feedbacks = feedbackRepository.findAllByResumeId(query.resumeId)
        val (sectionFeedbacks, overallFeedbacks) = feedbacks.partition { it.section != null }
        return FeedbackStatsResult(
            totalCount = feedbacks.size.toLong(),
            overall = groupStats(overallFeedbacks),
            sections =
                sectionFeedbacks
                    .groupBy { it.section!! }
                    .map { (section, group) ->
                        val stats = groupStats(group)
                        SectionFeedbackStats(
                            section = section,
                            count = stats.count,
                            averageRating = stats.averageRating,
                            tagCounts = stats.tagCounts,
                        )
                    },
        )
    }

    private fun groupStats(feedbacks: List<Feedback>): FeedbackGroupStats {
        val ratings = feedbacks.mapNotNull { it.rating }
        return FeedbackGroupStats(
            count = feedbacks.size.toLong(),
            averageRating = if (ratings.isEmpty()) null else ratings.average(),
            tagCounts =
                feedbacks
                    .flatMap { it.tags }
                    .groupingBy { it }
                    .eachCount()
                    .mapValues { it.value.toLong() },
        )
    }

    private fun findResume(resumeId: Long): Resume {
        val resume =
            resumeRepository.findById(resumeId)
                ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        if (resume.isDeleted()) throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "이력서를 찾을 수 없습니다.")
        return resume
    }
}
