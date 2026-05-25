package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.application.usecase.BlockDraftSummary
import com.atomiccv.resume.application.usecase.CreateBlockDraftCommand
import com.atomiccv.resume.application.usecase.CreateBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeCommand
import com.atomiccv.resume.application.usecase.DeleteAllBlockDraftsByTypeUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftCommand
import com.atomiccv.resume.application.usecase.DeleteBlockDraftUseCase
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsCommand
import com.atomiccv.resume.application.usecase.DeleteBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftQuery
import com.atomiccv.resume.application.usecase.GetBlockDraftUseCase
import com.atomiccv.resume.application.usecase.GetBlockDraftsQuery
import com.atomiccv.resume.application.usecase.GetBlockDraftsUseCase
import com.atomiccv.resume.application.usecase.UpdateBlockDraftCommand
import com.atomiccv.resume.application.usecase.UpdateBlockDraftUseCase
import com.atomiccv.resume.domain.model.BlockDraft
import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
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
@RequestMapping("/api/block-drafts")
class BlockDraftController(
    private val createBlockDraftUseCase: CreateBlockDraftUseCase,
    private val updateBlockDraftUseCase: UpdateBlockDraftUseCase,
    private val getBlockDraftsUseCase: GetBlockDraftsUseCase,
    private val getBlockDraftUseCase: GetBlockDraftUseCase,
    private val deleteBlockDraftUseCase: DeleteBlockDraftUseCase,
    private val deleteBlockDraftsUseCase: DeleteBlockDraftsUseCase,
    private val deleteAllBlockDraftsByTypeUseCase: DeleteAllBlockDraftsByTypeUseCase,
) {
    @PostMapping
    fun createDraft(
        authentication: Authentication,
        @Valid @RequestBody request: CreateBlockDraftRequest,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft =
            createBlockDraftUseCase.create(
                CreateBlockDraftCommand(
                    userId = userId,
                    blockType = request.blockType,
                    title = request.title,
                    contentJson = request.contentJson.toString(),
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @GetMapping
    fun getDrafts(
        authentication: Authentication,
        @RequestParam type: BlockType,
        @RequestParam(required = false) currentDraftId: Long?,
    ): ResponseEntity<ApiResponse<List<BlockDraftSummaryResponse>>> {
        val userId = resolveUserId(authentication)
        val summaries =
            getBlockDraftsUseCase.getDrafts(
                GetBlockDraftsQuery(userId = userId, blockType = type, currentDraftId = currentDraftId),
            )
        return ResponseEntity.ok(ApiResponse.ok(summaries.map { it.toResponse() }))
    }

    @GetMapping("/{id}")
    fun getDraft(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft = getBlockDraftUseCase.getDraft(GetBlockDraftQuery(draftId = id, userId = userId))
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @PutMapping("/{id}")
    fun updateDraft(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBlockDraftRequest,
    ): ResponseEntity<ApiResponse<BlockDraftResponse>> {
        val userId = resolveUserId(authentication)
        val draft =
            updateBlockDraftUseCase.update(
                UpdateBlockDraftCommand(
                    draftId = id,
                    userId = userId,
                    title = request.title,
                    contentJson = request.contentJson.toString(),
                ),
            )
        return ResponseEntity.ok(ApiResponse.ok(draft.toResponse()))
    }

    @DeleteMapping("/{id}")
    fun deleteDraft(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockDraftUseCase.delete(DeleteBlockDraftCommand(draftId = id, userId = userId))
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @DeleteMapping("/batch")
    fun deleteDrafts(
        authentication: Authentication,
        @Valid @RequestBody request: DeleteBlockDraftsRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteBlockDraftsUseCase.deleteAll(
            DeleteBlockDraftsCommand(draftIds = request.ids, userId = userId),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    @DeleteMapping
    fun deleteAllByType(
        authentication: Authentication,
        @RequestParam type: BlockType,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = resolveUserId(authentication)
        deleteAllBlockDraftsByTypeUseCase.deleteAll(
            DeleteAllBlockDraftsByTypeCommand(userId = userId, blockType = type),
        )
        return ResponseEntity.ok(ApiResponse.ok())
    }

    private fun resolveUserId(authentication: Authentication): Long =
        authentication.name.toLongOrNull()
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
}

data class CreateBlockDraftRequest(
    val blockType: BlockType,
    val title: String,
    val contentJson: JsonNode,
)

data class UpdateBlockDraftRequest(
    val title: String,
    val contentJson: JsonNode,
)

data class DeleteBlockDraftsRequest(
    @field:Size(min = 1)
    val ids: List<Long>,
)

data class BlockDraftResponse(
    val id: Long,
    val blockType: BlockType,
    val title: String,
    val contentJson: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class BlockDraftSummaryResponse(
    val id: Long,
    val title: String,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
)

fun BlockDraft.toResponse() =
    BlockDraftResponse(
        id = id,
        blockType = blockType,
        title = title,
        contentJson = contentJson,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BlockDraftSummary.toResponse() =
    BlockDraftSummaryResponse(
        id = id,
        title = title,
        updatedAt = updatedAt,
        isActive = isActive,
    )
