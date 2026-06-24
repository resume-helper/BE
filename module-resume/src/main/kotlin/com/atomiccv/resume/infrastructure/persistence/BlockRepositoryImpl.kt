package com.atomiccv.resume.infrastructure.persistence

import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.resume.domain.repository.BlockRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class BlockRepositoryImpl(
    private val jpaRepository: BlockJpaRepository,
) : BlockRepository {
    override fun save(block: Block): Block = jpaRepository.save(BlockJpaEntity.fromDomain(block)).toDomain()

    override fun saveAll(blocks: List<Block>): List<Block> =
        jpaRepository.saveAll(blocks.map { BlockJpaEntity.fromDomain(it) }).map { it.toDomain() }

    override fun findById(id: Long): Block? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findPageByUserId(
        userId: Long,
        page: Int,
        size: Int,
    ): Page<Block> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable).map { it.toDomain() }
    }

    override fun findPageByUserIdAndType(
        userId: Long,
        type: BlockType,
        page: Int,
        size: Int,
    ): Page<Block> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return jpaRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(userId, type, pageable).map { it.toDomain() }
    }
}
