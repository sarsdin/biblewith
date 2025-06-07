package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 모임 멤버 DTO */
data class GroupMemberDto(
    @SerializedName("group_no") val groupNo: Int,
    @SerializedName("user_no") val userNo: Int,
    @SerializedName("user_email") val userEmail: String?,
    @SerializedName("user_pwd") val userPwd: String?,
    @SerializedName("user_nick") val userNick: String,
    @SerializedName("user_create_date") val userCreateDate: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_image") val userImage: String?,
    @SerializedName("join_date") val joinDate: String?
)
