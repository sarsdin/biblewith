package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 게시물 댓글 DTO */
data class GboardReplyDto(
    @SerializedName("reply_no") val replyNo: Int,
    @SerializedName("gboard_no") val gboardNo: Int,
    @SerializedName("user_no") val userNo: Int,
    @SerializedName("reply_content") val replyContent: String,
    @SerializedName("reply_writedate") val replyWriteDate: String,
    @SerializedName("parent_reply_no") val parentReplyNo: Int?,
    @SerializedName("parent_nick") val parentNick: String?,
    @SerializedName("reply_group") val replyGroup: Int?,
    @SerializedName("user_nick") val userNick: String,
    @SerializedName("user_image") val userImage: String?
)
