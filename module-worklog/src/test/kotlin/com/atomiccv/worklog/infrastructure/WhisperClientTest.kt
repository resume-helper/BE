package com.atomiccv.worklog.infrastructure

import com.atomiccv.worklog.infrastructure.client.GptSummaryClient
import com.atomiccv.worklog.infrastructure.client.WhisperClient
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals

class WhisperClientTest {
    @Test
    fun `Whisper API 호출 시 transcript 텍스트를 반환한다`() {
        val builder = RestClient.builder().baseUrl("https://api.openai.com")
        val mockServer = MockRestServiceServer.bindTo(builder).build()

        mockServer
            .expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("안녕하세요 회의 내용입니다", MediaType("text", "plain", Charsets.UTF_8)))

        val client = WhisperClient(openAiRestClient = builder.build())
        val result = client.transcribe("audio".toByteArray(), "meeting.mp3")

        mockServer.verify()
        assertEquals("안녕하세요 회의 내용입니다", result)
    }

    @Test
    fun `GPT 요약 API 호출 시 요약 텍스트를 반환한다`() {
        val builder = RestClient.builder().baseUrl("https://api.openai.com")
        val mockServer = MockRestServiceServer.bindTo(builder).build()
        val responseJson =
            """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "• 핵심 결정: JWT 만료 7일로 변경\n• 액션: 홍길동 - PR 작성"
                  }
                }
              ]
            }
            """.trimIndent()

        mockServer
            .expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val client = GptSummaryClient(openAiRestClient = builder.build())
        val result = client.summarize("회의 전사 텍스트")

        mockServer.verify()
        assertEquals("• 핵심 결정: JWT 만료 7일로 변경\n• 액션: 홍길동 - PR 작성", result)
    }
}
