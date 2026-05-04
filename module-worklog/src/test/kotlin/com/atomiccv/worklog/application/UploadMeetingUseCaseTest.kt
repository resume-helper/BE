package com.atomiccv.worklog.application

import com.atomiccv.worklog.application.port.GptSummaryPort
import com.atomiccv.worklog.application.port.WhisperPort
import com.atomiccv.worklog.application.usecase.UploadMeetingCommand
import com.atomiccv.worklog.application.usecase.UploadMeetingUseCase
import com.atomiccv.worklog.domain.model.Meeting
import com.atomiccv.worklog.domain.model.MeetingStatus
import com.atomiccv.worklog.domain.repository.MeetingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class UploadMeetingUseCaseTest {
    private val whisperPort: WhisperPort = mockk()
    private val gptSummaryPort: GptSummaryPort = mockk()
    private val meetingRepository: MeetingRepository = mockk()
    private val useCase = UploadMeetingUseCase(whisperPort, gptSummaryPort, meetingRepository)

    @Test
    fun `음성 업로드 시 Whisper 변환, GPT 요약 후 Meeting이 저장된다`() {
        val command =
            UploadMeetingCommand(
                title = "스프린트 회의",
                meetingDate = LocalDate.of(2026, 5, 5),
                audioBytes = "audio-data".toByteArray(),
                fileName = "sprint.mp3",
            )
        val savedMeetingSlot = slot<Meeting>()

        every { whisperPort.transcribe(command.audioBytes, command.fileName) } returns "회의 내용입니다"
        every { gptSummaryPort.summarize("회의 내용입니다") } returns "• 주요 결정: 배포 일정 확정"
        every { meetingRepository.save(capture(savedMeetingSlot)) } answers {
            savedMeetingSlot.captured.copy(id = 1L)
        }

        val result = useCase.execute(command)

        assertEquals(1L, result.id)
        assertEquals("회의 내용입니다", savedMeetingSlot.captured.transcript)
        assertEquals("• 주요 결정: 배포 일정 확정", savedMeetingSlot.captured.summary)
        assertEquals(MeetingStatus.COMPLETED, savedMeetingSlot.captured.status)
    }

    @Test
    fun `Whisper API 실패 시 FAILED 상태로 Meeting이 저장된다`() {
        val command =
            UploadMeetingCommand(
                title = "회의",
                meetingDate = LocalDate.now(),
                audioBytes = ByteArray(0),
                fileName = "meeting.mp3",
            )
        val savedSlot = slot<Meeting>()

        every { whisperPort.transcribe(any(), any()) } throws RuntimeException("API 오류")
        every { meetingRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = 2L)
        }

        val result = useCase.execute(command)

        assertEquals(MeetingStatus.FAILED, savedSlot.captured.status)
        assertEquals(2L, result.id)
        verify(exactly = 0) { gptSummaryPort.summarize(any()) }
    }
}
