package com.atomiccv.shared.common.exception

open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.defaultMessage,
) : RuntimeException(message) {
    val code: String get() = errorCode.code
    val httpStatus: Int get() = errorCode.httpStatus
}
