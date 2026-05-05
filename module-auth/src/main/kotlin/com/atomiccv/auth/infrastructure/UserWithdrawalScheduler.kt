package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.persistence.SocialAccountJpaRepository
import com.atomiccv.auth.infrastructure.persistence.UserJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class UserWithdrawalScheduler(
    private val userJpaRepository: UserJpaRepository,
    private val socialAccountJpaRepository: SocialAccountJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun deleteExpiredWithdrawnUsers() {
        val cutoff = LocalDateTime.now().minusDays(30)
        val expiredUsers = userJpaRepository.findByDeletedAtBefore(cutoff)
        if (expiredUsers.isEmpty()) return

        val userIds = expiredUsers.map { it.id }
        socialAccountJpaRepository.deleteByUserIdIn(userIds)
        userJpaRepository.deleteAll(expiredUsers)

        log.info("탈퇴 유저 영구 삭제 완료: {}명", expiredUsers.size)
    }
}
