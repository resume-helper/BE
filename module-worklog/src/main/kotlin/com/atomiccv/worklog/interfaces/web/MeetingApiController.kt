package com.atomiccv.worklog.interfaces.web

import com.atomiccv.worklog.application.usecase.UploadMeetingCommand
import com.atomiccv.worklog.application.usecase.UploadMeetingUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

private val ALLOWED_AUDIO_TYPES = setOf("audio/mpeg", "audio/mp4", "audio/wav", "audio/x-wav")
private const val MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024

@Controller
@RequestMapping("/worklog/meetings")
class MeetingApiController(
    private val uploadMeetingUseCase: UploadMeetingUseCase,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestParam title: String,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        meetingDate: LocalDate,
        @RequestParam audioFile: MultipartFile,
        model: Model,
    ): String {
        val validationError = validateAudioFile(audioFile)
        if (validationError != null) {
            model.addAttribute("error", validationError)
            return "worklog/meeting-card :: error-card"
        }

        val meeting =
            uploadMeetingUseCase.execute(
                UploadMeetingCommand(
                    title = title.trim(),
                    meetingDate = meetingDate,
                    audioBytes = audioFile.bytes,
                    fileName = audioFile.originalFilename ?: "meeting.mp3",
                ),
            )
        model.addAttribute("meeting", meeting)
        return "worklog/meeting-card :: meeting-card"
    }

    private fun validateAudioFile(audioFile: MultipartFile): String? {
        val contentType = audioFile.contentType ?: ""
        return when {
            contentType !in ALLOWED_AUDIO_TYPES -> "지원하지 않는 파일 형식입니다. (mp3, mp4, wav만 허용)"
            audioFile.size > MAX_FILE_SIZE_BYTES -> "파일 크기는 25MB를 초과할 수 없습니다."
            else -> null
        }
    }
}
