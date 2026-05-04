package com.atomiccv.worklog.application.port

interface GptSummaryPort {
    fun summarize(transcript: String): String
}
