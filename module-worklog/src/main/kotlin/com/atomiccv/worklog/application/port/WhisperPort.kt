package com.atomiccv.worklog.application.port

interface WhisperPort {
    fun transcribe(
        audioBytes: ByteArray,
        fileName: String,
    ): String
}
