package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 모임 상세 응답 래퍼 */
data class GroupDetailResponse(
    @SerializedName("result") val result: GroupDetailDto,
    @SerializedName("msg") val msg: String?
)
