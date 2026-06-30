package com.atomiccv.shared.interfaces.rest

import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.atomiccv.shared.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(e.httpStatus).body(
            ApiResponse.error(code = e.code, message = e.message ?: e.errorCode.defaultMessage),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message =
            e.bindingResult.fieldErrors
                .firstOrNull()
                ?.defaultMessage
                ?: ErrorCode.VALIDATION_FAILED.defaultMessage
        return ResponseEntity.status(400).body(
            ApiResponse.error(code = ErrorCode.VALIDATION_FAILED.code, message = message),
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("Request body 파싱 실패: {}", e.message)
        return ResponseEntity.status(400).body(
            ApiResponse.error(code = ErrorCode.VALIDATION_FAILED.code, message = "요청 본문을 읽을 수 없습니다."),
        )
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("Resource not found: {}", e.message)
        return ResponseEntity.status(404).body(
            ApiResponse.error(
                code = ErrorCode.RESOURCE_NOT_FOUND.code,
                message = ErrorCode.RESOURCE_NOT_FOUND.defaultMessage,
            ),
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception", e)
        return ResponseEntity.status(500).body(
            ApiResponse.error(
                code = ErrorCode.INTERNAL_SERVER_ERROR.code,
                message = ErrorCode.INTERNAL_SERVER_ERROR.defaultMessage,
            ),
        )
    }
}
