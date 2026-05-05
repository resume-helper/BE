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
    private val socialAccountJpaRepository: SocialAccountJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun deleteExpiredWithdrawnAccounts() {
        val cutoff = LocalDateTime.now().minusDays(30)
        val expiredAccounts = socialAccountJpaRepository.findByDeletedAtBefore(cutoff)
        if (expiredAccounts.isEmpty()) return

        val affectedUserIds = expiredAccounts.map { it.userId }.toSet()
        socialAccountJpaRepository.deleteAll(expiredAccounts)
        log.info("만료된 소셜 계정 영구 삭제: {}건", expiredAccounts.size)

        val usersToDelete =
            affectedUserIds.filter { userId ->
                socialAccountJpaRepository.countByUserIdAndIsActiveTrue(userId) == 0 &&
                    socialAccountJpaRepository.findAllByUserId(userId).isEmpty()
            }
        if (usersToDelete.isNotEmpty()) {
            userJpaRepository.deleteAllById(usersToDelete)
            log.info("연결된 소셜 계정 없는 유저 영구 삭제: {}명", usersToDelete.size)
        }
    }
}
