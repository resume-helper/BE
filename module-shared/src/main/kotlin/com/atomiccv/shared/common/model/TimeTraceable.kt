package com.atomiccv.shared.common.model

import java.time.LocalDateTime

interface TimeTraceable {
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}
