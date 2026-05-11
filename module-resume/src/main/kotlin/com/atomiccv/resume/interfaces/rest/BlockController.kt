package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.CreateBlockCommand
import com.atomiccv.resume.application.usecase.CreateBlockUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockUseCase
import com.atomiccv.resume.application.usecase.GetBlocksQuery
import com.atomiccv.resume.application.usecase.GetBlocksUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockCommand
import com.atomiccv.resume.application.usecase.UpdateBlockUseCase
import com.atomiccv.resume.domain.model.Block
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/blocks")
class BlockController(
    private val createBlockUseCase: CreateBlockUseCase,
    private val updateBlockUseCase: UpdateBlockUseCase,
    private val deleteBlockUseCase: DeleteBlockUseCase,
    private val getBlocksUseCase: GetBlocksUseCase,
) {
    @GetMapping
    fun getBlocks(
        authentication: Authentication,
        @RequestParam(required = false) type: BlockType?,
    ): ResponseEntity<ApiResponse<List<BlockResponse>>> {
        val userId = resolveUserId(authentication)
        val blocks = getBlocksUseCase.getBlocks(GetBlocksQuery(userId = userId, type = type))
        return ResponseEntity.ok(ApiResponse.ok(blocks.map { it.toResponse() }))
    }

    @PostMapping
    fun createBlock(
        authentication: Authentication,
        @Valid @RequestBody request: CreateBlockRequest,
    ): ResponseEntity<ApiResponse<BlockResponse>> {
        val userId = resolveUserId(authentication)
        val block =
            createBlockUseCase.create(
                CreateBlockCommand(
                    userId = userId,
                    type = request.type,
                    title = request.title,
                    contentJson = request.contentJson,
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(block.toResponse()))
    }

    @PutMapping("/{id}")
    fun updateBlock(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBlockRequest,
    ): ResponseEntity<ApiResponse<BlockResponse>> {
        val userId = resolveUserId(authentication)
        val block =
            updateBlockUseCase.update(
                UpdateBlockCommand(
                    blockId = id,
                    userId = userId,
                    title = request.title,
                    contentJson = request.contentJson,
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(block.toResponse()))
    }

    @DeleteMapping("/{id}")
    fun deleteBlock(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockUseCase.delete(blockId = id, userId = userId)
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
}

data class CreateBlockRequest(
    val type: BlockType,
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:NotBlank
    val contentJson: String,
)

data class UpdateBlockRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:NotBlank
    val contentJson: String,
)

data class BlockResponse(
    val id: Long,
    val type: BlockType,
    val title: String,
    val contentJson: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun Block.toResponse() =
    BlockResponse(
        id = id,
        type = type,
        title = title,
        contentJson = contentJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
