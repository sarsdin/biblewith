package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 모임 목록 응답 래퍼 */
data class GroupListResponse(
    @SerializedName("result") val result: List<GroupInfoDto>,
    @SerializedName("msg") val msg: String?
)
