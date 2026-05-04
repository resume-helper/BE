package com.atomiccv.worklog.interfaces.web

import com.atomiccv.worklog.application.usecase.GetDailyWorklogUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Controller
@RequestMapping("/worklog")
class WorklogController(
    private val getDailyWorklogUseCase: GetDailyWorklogUseCase,
) {
    @GetMapping
    fun dashboard(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
        model: Model,
    ): String {
        val targetDate = date ?: LocalDate.now()
        val worklog = getDailyWorklogUseCase.execute(targetDate)
        model.addAttribute("worklog", worklog)
        model.addAttribute("prevDate", targetDate.minusDays(1))
        model.addAttribute("nextDate", targetDate.plusDays(1))
        model.addAttribute("isToday", targetDate == LocalDate.now())
        return "worklog/dashboard"
    }
}
