package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeDetail
import com.atomiccv.resume.domain.repository.ResumeRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetPublicResumeUseCaseTest {
    private val resumeRepository: ResumeRepository = mockk()
    private val useCase = GetPublicResumeUseCase(resumeRepository)

    private fun resumeFixture(isPublic: Boolean = true) =
        Resume(
            id = 1L,
            userId = 1L,
            title = "공개 이력서",
            slug = "abc123",
            isPublic = isPublic,
        )

    @Test
    fun `공개 이력서를 slug로 조회하면 blocks 포함 상세를 반환한다`() {
        val resume = resumeFixture()
        every { resumeRepository.findBySlug("abc123") } returns resume
        every { resumeRepository.findDetailById(1L) } returns ResumeDetail(resume = resume, blocks = emptyList())

        val result = useCase.getDetail(GetPublicResumeQuery(slug = "abc123"))

        assertEquals(1L, result.resume.id)
        assertEquals("공개 이력서", result.resume.title)
        assertEquals(emptyList(), result.blocks)
    }

    @Test
    fun `존재하지 않는 slug면 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findBySlug("missing") } returns null

        val exception =
            assertFailsWith<BusinessException> {
                useCase.getDetail(GetPublicResumeQuery(slug = "missing"))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `비공개 이력서면 존재를 노출하지 않도록 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findBySlug("abc123") } returns resumeFixture(isPublic = false)

        val exception =
            assertFailsWith<BusinessException> {
                useCase.getDetail(GetPublicResumeQuery(slug = "abc123"))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `상세 조회 결과가 없으면 RESUME_NOT_FOUND 예외가 발생한다`() {
        every { resumeRepository.findBySlug("abc123") } returns resumeFixture()
        every { resumeRepository.findDetailById(1L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                useCase.getDetail(GetPublicResumeQuery(slug = "abc123"))
            }
        assertEquals(ErrorCode.RESUME_NOT_FOUND, exception.errorCode)
    }
}
