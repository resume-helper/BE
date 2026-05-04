package com.atomiccv.worklog.infrastructure.client

import com.atomiccv.worklog.application.port.WhisperPort
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

class WhisperClient(
    private val openAiRestClient: RestClient,
) : WhisperPort {
    override fun transcribe(
        audioBytes: ByteArray,
        fileName: String
    ): String {
        val body = LinkedMultiValueMap<String, Any>()
        body.add(
            "file",
            object : ByteArrayResource(audioBytes) {
                override fun getFilename() = fileName
            }
        )
        body.add("model", "whisper-1")
        body.add("language", "ko")
        body.add("response_format", "text")

        return openAiRestClient
            .post()
            .uri("/v1/audio/transcriptions")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(String::class.java)
            ?: error("Whisper API 응답이 없습니다")
    }
}
