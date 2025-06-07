package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 게시물 상세 result */
data class GboardDetailDto(
    @SerializedName("gboardInfo") val gboardInfo: GboardDto,
    @SerializedName("gboardReplyL") val gboardReplyL: List<GboardReplyDto>
)
