package com.atomiccv.shared.common.exception

enum class ErrorCode(val httpStatus: Int, val code: String, val defaultMessage: String) {
    VALIDATION_FAILED(400, "VALIDATION_FAILED", "입력값 유효성 검증 실패"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다"),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다"),
    RATE_LIMIT_EXCEEDED(429, "RATE_LIMIT_EXCEEDED", "요청 횟수를 초과했습니다"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),
}
