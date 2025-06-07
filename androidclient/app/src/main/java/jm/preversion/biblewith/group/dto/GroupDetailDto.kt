package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 모임 상세 응답 result */
data class GroupDetailDto(
    @SerializedName("0") val groupInfo: GroupInfoDto,
    @SerializedName("gboardL") val gboardL: List<GboardDto>,
    @SerializedName("memberL") val memberL: List<GroupMemberDto>
)
