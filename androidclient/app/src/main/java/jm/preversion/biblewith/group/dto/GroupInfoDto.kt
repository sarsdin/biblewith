package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 모임 정보 DTO */
data class GroupInfoDto(
    @SerializedName("group_no") val groupNo: Int,
    @SerializedName("chat_room_no") val chatRoomNo: Int?,
    @SerializedName("user_no") val userNo: Int,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("group_desc") val groupDesc: String?,
    @SerializedName("group_main_image") val groupMainImage: String?,
    @SerializedName("create_date") val createDate: String?
)
