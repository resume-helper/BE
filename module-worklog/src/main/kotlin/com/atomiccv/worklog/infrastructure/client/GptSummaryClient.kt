package com.atomiccv.worklog.infrastructure.client

import com.atomiccv.worklog.application.port.GptSummaryPort
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

data class GptMessage(
    val role: String,
    val content: String
)

data class GptRequest(
    val model: String,
    val messages: List<GptMessage>,
    @JsonProperty("max_tokens") val maxTokens: Int = 500,
)

data class GptChoice(
    val message: GptMessage
)

data class GptResponse(
    val choices: List<GptChoice>
)

class GptSummaryClient(
    private val openAiRestClient: RestClient,
) : GptSummaryPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun summarize(transcript: String): String {
        val request =
            GptRequest(
                model = "gpt-4o-mini",
                messages =
                    listOf(
                        GptMessage(
                            role = "system",
                            content =
                                "당신은 회의 내용을 한국어로 요약하는 어시스턴트입니다. " +
                                    "핵심 결정사항과 액션 아이템을 중심으로 3~5개 bullet point(•)로 요약하세요.",
                        ),
                        GptMessage(role = "user", content = transcript),
                    ),
            )
        return try {
            openAiRestClient
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GptResponse::class.java)
                ?.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?: "요약 실패"
        } catch (e: Exception) {
            log.warn("GPT 요약 실패: ${e.message}")
            "요약 실패 — 다시 시도해주세요"
        }
    }
}
