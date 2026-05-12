package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.stereotype.Repository

@Repository
class BlockRepositoryImpl(
    private val jpaRepository: BlockJpaRepository,
) : BlockRepository {
    override fun save(block: Block): Block = jpaRepository.save(BlockJpaEntity.fromDomain(block)).toDomain()

    override fun findById(id: Long): Block? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllActiveByUserId(userId: Long): List<Block> =
        jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId).map { it.toDomain() }

    override fun findAllActiveByUserIdAndType(
        userId: Long,
        type: BlockType
    ): List<Block> = jpaRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(userId, type).map { it.toDomain() }
}
