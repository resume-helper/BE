package com.atomiccv.shared.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 생성/수정 시각을 자동 관리하는 JPA 공통 베이스 엔티티.
 * JPA Auditing이 INSERT/UPDATE 시점에 자동으로 값을 주입한다.
 *
 * 사용 대상: createdAt + updatedAt 이 모두 필요한 엔티티
 * 사용 제외: createdAt만 필요한 엔티티 (SocialAccountJpaEntity 등) — @CreatedDate 직접 선언
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseJpaEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        protected set
}
