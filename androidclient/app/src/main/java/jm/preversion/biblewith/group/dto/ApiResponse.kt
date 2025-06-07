package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/**
 * 공통 API 응답 래퍼
 */
data class ApiResponse<T>(
    @SerializedName("result") val result: T?,
    @SerializedName("msg") val msg: String?
)
