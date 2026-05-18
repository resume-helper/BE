package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.model.ResumeType
import com.atomiccv.resume.domain.repository.ResumeListQuery
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.resume.domain.repository.SortDirection
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetResumesUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val useCase = GetResumesUseCase(resumeRepository)

    private val emptyPage = PageImpl<Resume>(emptyList(), PageRequest.of(0, 10), 0)

    @Test
    fun `1페이지 요청 시 page=0으로 변환하여 조회된다`() {
        val querySlot = slot<ResumeListQuery>()
        every { resumeRepository.findPageByUserId(any(), capture(querySlot)) } returns emptyPage

        useCase.getList(
            GetResumesQuery(
                userId = 1L,
                type = null,
                titleKeyword = null,
                page = 1,
                size = 10,
                sortDirection = SortDirection.NEWEST,
            ),
        )

        assertEquals(0, querySlot.captured.page)
    }

    @Test
    fun `type 필터 없으면 전체 조회 쿼리가 실행된다`() {
        val querySlot = slot<ResumeListQuery>()
        every { resumeRepository.findPageByUserId(any(), capture(querySlot)) } returns emptyPage

        useCase.getList(
            GetResumesQuery(
                userId = 1L,
                type = null,
                titleKeyword = null,
                page = 1,
                size = 10,
                sortDirection = SortDirection.NEWEST,
            ),
        )

        assertNull(querySlot.captured.type)
    }

    @Test
    fun `title 키워드로 필터링한다`() {
        val querySlot = slot<ResumeListQuery>()
        every { resumeRepository.findPageByUserId(any(), capture(querySlot)) } returns emptyPage

        useCase.getList(
            GetResumesQuery(
                userId = 1L,
                type = null,
                titleKeyword = "카카오",
                page = 1,
                size = 10,
                sortDirection = SortDirection.NEWEST,
            ),
        )

        assertEquals("카카오", querySlot.captured.titleKeyword)
    }

    @Test
    fun `type 필터를 지정하면 쿼리에 포함된다`() {
        val querySlot = slot<ResumeListQuery>()
        every { resumeRepository.findPageByUserId(any(), capture(querySlot)) } returns emptyPage

        useCase.getList(
            GetResumesQuery(
                userId = 1L,
                type = ResumeType.PDF,
                titleKeyword = null,
                page = 2,
                size = 5,
                sortDirection = SortDirection.OLDEST,
            ),
        )

        assertEquals(ResumeType.PDF, querySlot.captured.type)
        assertEquals(1, querySlot.captured.page)
        assertEquals(5, querySlot.captured.size)
    }
}
