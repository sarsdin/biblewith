package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 게시물 DTO */
data class GboardDto(
    @SerializedName("gboard_no") val gboardNo: Int,
    @SerializedName("group_no") val groupNo: Int,
    @SerializedName("user_no") val userNo: Int,
    @SerializedName("gboard_title") val gboardTitle: String?,
    @SerializedName("gboard_content") val gboardContent: String,
    @SerializedName("create_date") val createDate: String?,
    @SerializedName("user_email") val userEmail: String?,
    @SerializedName("user_pwd") val userPwd: String?,
    @SerializedName("user_nick") val userNick: String,
    @SerializedName("user_create_date") val userCreateDate: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_image") val userImage: String?,
    @SerializedName("is_like") val isLike: Boolean?,
    @SerializedName("gboard_like_count") val gboardLikeCount: String?,
    @SerializedName("reply_count") val replyCount: String?,
    @SerializedName("gboard_image") val gboardImage: List<GboardImageDto> = emptyList(),
    @SerializedName("replyL") val replyL: List<GboardReplyDto>? = null
)
