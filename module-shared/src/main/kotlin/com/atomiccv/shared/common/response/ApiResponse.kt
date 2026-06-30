package com.atomiccv.shared.common.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val code: String? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun ok(): ApiResponse<Nothing> = ApiResponse(success = true)

        fun error(
            code: String,
            message: String,
        ): ApiResponse<Nothing> = ApiResponse(success = false, code = code, message = message)
    }
}
