package com.atomiccv.auth.interfaces.rest

import com.atomiccv.shared.common.exception.BusinessException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(e.httpStatus).body(
            ErrorResponse(
                code = e.code,
                message = e.message ?: e.errorCode.defaultMessage,
                timestamp = LocalDateTime.now(),
            ),
        )

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(500).body(
            ErrorResponse(
                code = "INTERNAL_SERVER_ERROR",
                message = "서버 내부 오류가 발생했습니다.",
                timestamp = LocalDateTime.now(),
            ),
        )
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime,
)
