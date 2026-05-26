package com.atomiccv.resume.application.usecase

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateBlockDraftUseCaseTest {
    private val blockDraftRepository: BlockDraftRepository = mockk()
    private val useCase = CreateBlockDraftUseCase(blockDraftRepository)

    private val saved =
        BlockDraft(
            id = 1L,
            userId = 10L,
            blockType = BlockType.CAREER,
            title = "카카오 백엔드 개발자",
            contentJson = """{"company":"카카오"}""",
            expiresAt = LocalDateTime.now().plusDays(14),
        )

    @Test
    fun `임시저장 생성 시 userId·blockType·title·contentJson이 저장되고 반환된다`() {
        val command =
            CreateBlockDraftCommand(
                userId = 10L,
                blockType = BlockType.CAREER,
                title = "카카오 백엔드 개발자",
                contentJson = """{"company":"카카오"}""",
            )
        every { blockDraftRepository.save(any()) } returns saved

        val result = useCase.create(command)

        assertEquals(1L, result.id)
        verify {
            blockDraftRepository.save(
                match {
                    it.userId == 10L &&
                        it.blockType == BlockType.CAREER &&
                        it.title == "카카오 백엔드 개발자"
                }
            )
        }
    }

    @Test
    fun `생성 시 expiresAt은 현재 시각 기준 14일 후로 설정된다`() {
        val command =
            CreateBlockDraftCommand(
                userId = 10L,
                blockType = BlockType.CAREER,
                title = "제목",
                contentJson = "{}",
            )
        every { blockDraftRepository.save(any()) } answers { firstArg() }

        val result = useCase.create(command)

        assertTrue(result.expiresAt.isAfter(LocalDateTime.now().plusDays(13)))
    }

    @Test
    fun `title이 빈 문자열이면 BLOCK_DRAFT_TITLE_REQUIRED 예외가 발생한다`() {
        val command =
            CreateBlockDraftCommand(
                userId = 10L,
                blockType = BlockType.CAREER,
                title = "   ",
                contentJson = "{}",
            )

        val ex = assertFailsWith<BusinessException> { useCase.create(command) }
        assertEquals(ErrorCode.BLOCK_DRAFT_TITLE_REQUIRED, ex.errorCode)
    }
}
