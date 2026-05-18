package com.atomiccv.shared.interfaces.rest

import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(e.httpStatus).body(
            ErrorResponse(
                code = e.code,
                message = e.message ?: e.errorCode.defaultMessage,
                timestamp = LocalDateTime.now(),
            ),
        )

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        log.warn("Resource not found: {}", e.message)
        return ResponseEntity.status(404).body(
            ErrorResponse(
                code = ErrorCode.RESOURCE_NOT_FOUND.code,
                message = ErrorCode.RESOURCE_NOT_FOUND.defaultMessage,
                timestamp = LocalDateTime.now(),
            ),
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", e)
        return ResponseEntity.status(500).body(
            ErrorResponse(
                code = "INTERNAL_SERVER_ERROR",
                message = "서버 내부 오류가 발생했습니다.",
                timestamp = LocalDateTime.now(),
            ),
        )
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime,
)
