package com.atomiccv.resume.infrastructure.scheduler

import com.atomiccv.resume.domain.repository.BlockDraftRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BlockDraftExpireScheduler(
    private val blockDraftRepository: BlockDraftRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun deleteExpiredDrafts() {
        val deleted = blockDraftRepository.deleteAllExpiredBefore(LocalDateTime.now())
        log.info("만료 임시저장 삭제 완료: {}건", deleted)
    }
}
