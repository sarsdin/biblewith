package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 게시물 상세 응답 */
data class GboardDetailResponse(
    @SerializedName("result") val result: GboardDetailDto,
    @SerializedName("msg") val msg: String?
)
