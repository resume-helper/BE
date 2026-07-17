package com.atomiccv.resume.domain.model

/**
 * 피드백 선택형 태그 카탈로그 (SSOT = docs/planning/feedback-feature.md).
 *
 * - 섹션별 피드백: 섹션(BlockType) 카테고리마다 허용 태그가 다르다.
 * - 전체 피드백(section = null): OVERALL_TAGS 만 허용한다.
 * - 카탈로그에 없는 태그는 제출 시 거부된다. 기획에서 태그 문구가 바뀌면 여기도 함께 바꾼다.
 */
object FeedbackTagCatalog {
    val SECTION_TAGS: Map<BlockType, Set<String>> =
        mapOf(
            BlockType.BASIC_INFO to emptySet(),
            BlockType.SUMMARY to
                setOf(
                    "핵심이 잘 담겼어요",
                    "인상적이었어요",
                    "차별점이 느껴져요",
                    "너무 길어요",
                    "강점이 잘 안 보여요",
                    "차별점이 없어요",
                ),
            BlockType.INTRODUCTION to
                setOf(
                    "개성이 느껴져요",
                    "지원 의지가 보여요",
                    "설득력 있어요",
                    "직무 연관성이 낮아요",
                    "구체성이 부족해요",
                    "내용이 너무 많아요",
                ),
            BlockType.SKILL to
                setOf(
                    "스택이 잘 정리됐어요",
                    "역량이 잘 보여요",
                    "직무에 잘 맞아요",
                    "스택이 너무 많아요",
                    "수준을 알기 어려워요",
                    "직무 연관성이 낮아요",
                ),
            BlockType.CAREER to
                setOf(
                    "성과가 잘 드러나요",
                    "역할이 명확해요",
                    "읽기 쉬웠어요",
                    "성과 수치가 부족해요",
                    "역할이 모호해요",
                    "내용이 너무 많아요",
                ),
            BlockType.PROJECT to
                setOf(
                    "기여도가 잘 보여요",
                    "결과가 명확해요",
                    "문제 해결 과정이 인상적이에요",
                    "기여도가 불분명해요",
                    "결과가 잘 안 보여요",
                    "내용이 너무 많아요",
                ),
            BlockType.EDUCATION to
                setOf(
                    "한눈에 파악돼요",
                    "잘 정리됐어요",
                    "정보가 부족해요",
                    "불필요한 내용이 있어요",
                ),
            BlockType.ACTIVITY to
                setOf(
                    "경험이 다양해요",
                    "성장이 느껴져요",
                    "직무 연관성이 높아요",
                    "설명이 부족해요",
                    "정리가 필요해요",
                    "직무 연관성이 낮아요",
                ),
            BlockType.CERTIFICATE to
                setOf(
                    "역량을 잘 뒷받침해요",
                    "직무 연관성이 높아요",
                    "설명이 부족해요",
                    "직무 연관성이 낮아요",
                ),
        )

    val OVERALL_TAGS: Set<String> =
        setOf(
            "전체적으로 잘 읽혀요",
            "강점이 잘 드러나요",
            "구성이 명확해요",
            "완성도가 높아요",
            "내용 보충이 필요해요",
            "핵심이 잘 안 보여요",
            "가독성이 아쉬워요",
            "직무 연관성이 낮아요",
        )

    /** 해당 섹션(null = 전체 피드백)에서 허용되는 태그 집합 */
    fun allowedTags(section: BlockType?): Set<String> = section?.let { SECTION_TAGS[it].orEmpty() } ?: OVERALL_TAGS

    /** 섹션 피드백을 남길 수 있는 섹션인지 (CUSTOM 등 카탈로그 밖 타입은 불가) */
    fun isFeedbackSection(type: BlockType): Boolean = SECTION_TAGS.containsKey(type)
}
