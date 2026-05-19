package com.atomiccv.resume.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "feedback_tags")
class FeedbackTagJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "feedback_id", nullable = false)
    val feedbackId: Long,
    @Column(nullable = false, length = 50)
    val tag: String,
)
