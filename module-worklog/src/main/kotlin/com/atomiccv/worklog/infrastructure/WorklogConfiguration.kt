package com.atomiccv.worklog.infrastructure

import com.atomiccv.worklog.application.port.GithubActivityPort
import com.atomiccv.worklog.application.port.GptSummaryPort
import com.atomiccv.worklog.application.port.WhisperPort
import com.atomiccv.worklog.application.usecase.GetDailyWorklogUseCase
import com.atomiccv.worklog.application.usecase.GetGithubActivityUseCase
import com.atomiccv.worklog.application.usecase.UploadMeetingUseCase
import com.atomiccv.worklog.domain.repository.MeetingRepository
import com.atomiccv.worklog.infrastructure.client.GithubClient
import com.atomiccv.worklog.infrastructure.client.GptSummaryClient
import com.atomiccv.worklog.infrastructure.client.WhisperClient
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.util.concurrent.TimeUnit

@Configuration
class WorklogConfiguration {
    @Bean
    fun openAiRestClient(
        @Value("\${worklog.openai.api-key}") apiKey: String,
    ): RestClient =
        RestClient
            .builder()
            .baseUrl("https://api.openai.com")
            .defaultHeader("Authorization", "Bearer $apiKey")
            .build()

    @Bean
    fun githubRestClient(
        @Value("\${worklog.github.token}") token: String,
    ): RestClient =
        RestClient
            .builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Authorization", "Bearer $token")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build()

    @Bean
    fun githubActivityCache(): Cache<String, Any> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build()

    @Bean
    fun whisperPort(openAiRestClient: RestClient): WhisperPort = WhisperClient(openAiRestClient)

    @Bean
    fun gptSummaryPort(openAiRestClient: RestClient): GptSummaryPort = GptSummaryClient(openAiRestClient)

    @Bean
    fun githubActivityPort(
        githubRestClient: RestClient,
        githubActivityCache: Cache<String, Any>,
        @Value("\${worklog.github.org}") org: String,
    ): GithubActivityPort =
        GithubClient(
            githubRestClient = githubRestClient,
            cache = githubActivityCache,
            org = org,
        )

    @Bean
    fun uploadMeetingUseCase(
        whisperPort: WhisperPort,
        gptSummaryPort: GptSummaryPort,
        meetingRepository: MeetingRepository,
    ): UploadMeetingUseCase = UploadMeetingUseCase(whisperPort, gptSummaryPort, meetingRepository)

    @Bean
    fun getGithubActivityUseCase(githubActivityPort: GithubActivityPort): GetGithubActivityUseCase =
        GetGithubActivityUseCase(githubActivityPort)

    @Bean
    fun getDailyWorklogUseCase(
        meetingRepository: MeetingRepository,
        githubActivityPort: GithubActivityPort,
    ): GetDailyWorklogUseCase = GetDailyWorklogUseCase(meetingRepository, githubActivityPort)
}
