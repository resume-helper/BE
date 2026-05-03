# API 응답 포맷 명세

> 출처: doc/discussion.md [3] 확정

## 성공 응답 (공통 래퍼)

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

## 에러 응답

```json
{
  "code": "VALIDATION_FAILED",
  "message": "이메일 형식이 올바르지 않습니다.",
  "errors": [
    { "field": "email", "message": "올바른 이메일 형식을 입력해주세요." }
  ]
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
| 502 | `OAUTH2_PROVIDER_ERROR` | 소셜 로그인 제공자 응답 오류 |

## Kotlin 구현 참조

```kotlin
// 공통 응답 래퍼
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

// 비즈니스 예외 base
open class BusinessException(
    val code: String,
    override val message: String
) : RuntimeException(message)

// 도메인 예외 예시
class ResumeNotFoundException(resumeId: Long) :
    BusinessException("RESOURCE_NOT_FOUND", "이력서를 찾을 수 없습니다. id=$resumeId")

// 전역 핸들러
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handle(e: BusinessException): ResponseEntity<ErrorResponse> { ... }
}
```
