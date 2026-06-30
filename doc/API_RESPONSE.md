# API 응답 포맷 명세

> 출처: doc/discussion.md [3] 확정
> 2026-06-30 업데이트: 에러 응답도 ApiResponse<T>로 통일 (ErrorResponse 제거)

## 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "message": null,
  "code": null
}
```

## 에러 응답

성공·에러 모두 동일한 `ApiResponse<T>` 래퍼를 사용한다.

```json
{
  "success": false,
  "data": null,
  "message": "이메일 형식이 올바르지 않습니다.",
  "code": "VALIDATION_FAILED"
}
```

## 비즈니스 에러코드

| HTTP | 에러 코드 | 설명 |
|------|----------|------|
| 400 | `VALIDATION_FAILED` | 입력값 유효성 검증 실패 |
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 |
| 401 | `TOKEN_EXPIRED` | Access Token 만료 |
| 401 | `INVALID_TOKEN` | 토큰 형식 오류 또는 위변조 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `RESOURCE_NOT_FOUND` | 리소스 없음 |
| 409 | `DUPLICATE_EMAIL` | 이메일 중복 |
| 429 | `RATE_LIMIT_EXCEEDED` | Rate Limit 초과 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |
| 400 | `BLOCK_DRAFT_TITLE_REQUIRED` | 임시저장 제목 누락 |
| 403 | `BLOCK_DRAFT_FORBIDDEN` | 임시저장 접근 권한 없음 |
| 404 | `BLOCK_DRAFT_NOT_FOUND` | 임시저장 없음 또는 만료 |

## Kotlin 구현 참조

```kotlin
// 공통 응답 래퍼 (성공·에러 공용)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val code: String? = null,   // 에러 시만 값이 채워짐
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun ok(): ApiResponse<Nothing> = ApiResponse(success = true)
        fun error(code: String, message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, code = code, message = message)
    }
}

// 비즈니스 예외 base
open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.defaultMessage,
) : RuntimeException(message) {
    val code: String get() = errorCode.code
    val httpStatus: Int get() = errorCode.httpStatus
}

// 도메인 예외 예시
class ResumeNotFoundException(resumeId: Long) :
    BusinessException(ErrorCode.RESUME_NOT_FOUND, "이력서를 찾을 수 없습니다. id=$resumeId")

// 전역 핸들러 (module-shared)
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(e.httpStatus).body(
            ApiResponse.error(code = e.code, message = e.message ?: e.errorCode.defaultMessage)
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: ErrorCode.VALIDATION_FAILED.defaultMessage
        return ResponseEntity.status(400).body(
            ApiResponse.error(code = ErrorCode.VALIDATION_FAILED.code, message = message)
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(500).body(
            ApiResponse.error(code = ErrorCode.INTERNAL_SERVER_ERROR.code, message = ErrorCode.INTERNAL_SERVER_ERROR.defaultMessage)
        )
}

// Spring Security authenticationEntryPoint (SecurityConfig)
// — 필터 레이어에서 401 직접 응답 시에도 ApiResponse 사용
it.authenticationEntryPoint { _, response, _ ->
    response.status = 401
    response.contentType = "application/json;charset=UTF-8"
    response.writer.write(
        objectMapper.writeValueAsString(
            ApiResponse.error(code = ErrorCode.UNAUTHORIZED.code, message = ErrorCode.UNAUTHORIZED.defaultMessage)
        )
    )
}
```
