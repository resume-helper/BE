package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockDraftRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BlockDraftRepositoryImpl(
    private val jpaRepository: BlockDraftJpaRepository,
) : BlockDraftRepository {
    override fun save(draft: BlockDraft): BlockDraft =
        jpaRepository.save(BlockDraftJpaEntity.fromDomain(draft)).toDomain()

    override fun findById(id: Long): BlockDraft? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllByUserIdAndBlockType(
        userId: Long,
        blockType: BlockType
    ): List<BlockDraft> =
        jpaRepository.findAllByUserIdAndBlockTypeOrderByUpdatedAtDesc(userId, blockType).map { it.toDomain() }

    override fun findAllByIds(ids: List<Long>): List<BlockDraft> = jpaRepository.findAllById(ids).map { it.toDomain() }

    override fun deleteById(id: Long) = jpaRepository.deleteById(id)

    override fun deleteAllByIds(ids: List<Long>) = jpaRepository.deleteAllByIdInBatch(ids)

    override fun deleteAllByUserIdAndBlockType(
        userId: Long,
        blockType: BlockType,
    ) = jpaRepository.deleteAllByUserIdAndBlockType(userId, blockType)

    override fun deleteAllExpiredBefore(threshold: LocalDateTime): Int = jpaRepository.deleteAllExpiredBefore(threshold)
}
