package com.atomiccv.worklog.application.usecase

import com.atomiccv.worklog.application.port.GptSummaryPort
import com.atomiccv.worklog.application.port.WhisperPort
import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.model.MeetingStatus
import com.atomiccv.worklog.domain.repository.MeetingRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate

data class UploadMeetingCommand(
    val title: String,
    val meetingDate: LocalDate,
    val audioBytes: ByteArray,
    val fileName: String,
)

class UploadMeetingUseCase(
    private val whisperPort: WhisperPort,
    private val gptSummaryPort: GptSummaryPort,
    private val meetingRepository: MeetingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Suppress("TooGenericExceptionCaught")
    fun execute(command: UploadMeetingCommand): Meeting =
        try {
            val transcript = whisperPort.transcribe(command.audioBytes, command.fileName)
            val summary = gptSummaryPort.summarize(transcript)
            meetingRepository.save(
                Meeting(
                    title = command.title,
                    meetingDate = command.meetingDate,
                    transcript = transcript,
                    summary = summary,
                    status = MeetingStatus.COMPLETED,
                ),
            )
        } catch (e: Exception) {
            log.error("회의 업로드 처리 실패: ${command.title}", e)
            meetingRepository.save(
                Meeting(
                    title = command.title,
                    meetingDate = command.meetingDate,
                    summary = "요약 실패 — 다시 시도해주세요",
                    status = MeetingStatus.FAILED,
                ),
            )
        }
}
