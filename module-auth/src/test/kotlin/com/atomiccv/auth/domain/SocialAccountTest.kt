package com.atomiccv.auth.domain

import com.atomiccv.auth.domain.model.SocialAccount
import com.atomiccv.auth.domain.model.SocialProvider
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocialAccountTest {
    private val base = SocialAccount(userId = 1L, provider = SocialProvider.GOOGLE, providerUserId = "g-1")

    @Test
    fun `활성 계정은 유예기간 내로 판단하지 않는다`() {
        assertFalse(base.isWithinGracePeriod())
    }

    @Test
    fun `30일 이내에 삭제된 계정은 유예기간 내다`() {
        val recent = base.copy(isActive = false, deletedAt = LocalDateTime.now().minusDays(10))
        assertTrue(recent.isWithinGracePeriod())
    }

    @Test
    fun `30일이 지난 삭제 계정은 유예기간이 만료됐다`() {
        val expired = base.copy(isActive = false, deletedAt = LocalDateTime.now().minusDays(31))
        assertFalse(expired.isWithinGracePeriod())
    }
}
