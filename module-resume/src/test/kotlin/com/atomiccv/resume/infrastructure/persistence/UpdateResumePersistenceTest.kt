package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.application.usecase.UpdateResumeCommand
import com.atomiccv.resume.application.usecase.UpdateResumeUseCase
import com.atomiccv.resume.domain.model.Resume
import com.atomiccv.resume.domain.repository.ResumeRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

@DataJpaTest
@Import(ResumeRepositoryImpl::class)
class UpdateResumePersistenceTest {
    @Autowired
    private lateinit var em: TestEntityManager

    @Autowired
    private lateinit var resumeRepository: ResumeRepository

    @Test
    fun `이력서 수정 시 title 변경이 실제 DB에 반영된다`() {
        val saved = resumeRepository.save(Resume(userId = 1L, title = "원래제목"))
        em.flush()
        em.clear()

        UpdateResumeUseCase(resumeRepository).update(
            UpdateResumeCommand(
                resumeId = saved.id,
                userId = 1L,
                title = "변경된제목",
                blocks = emptyList(),
            ),
        )
        em.flush()
        em.clear()

        val reloaded = resumeRepository.findById(saved.id)
        assertEquals("변경된제목", reloaded?.title)
    }
}
