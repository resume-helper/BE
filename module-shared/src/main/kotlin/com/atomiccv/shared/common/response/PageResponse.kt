package com.atomiccv.shared.common.response

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
) {
    companion object {
        fun <T> from(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                page = page.number + 1,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
            )

        fun <T, R> from(
            page: Page<T>,
            transform: (T) -> R,
        ): PageResponse<R> =
            PageResponse(
                content = page.content.map(transform),
                page = page.number + 1,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
            )
    }
}
