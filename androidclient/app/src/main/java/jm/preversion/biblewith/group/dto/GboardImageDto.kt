package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

/** 게시물 이미지 DTO */
data class GboardImageDto(
    @SerializedName("gboard_image_no") val imageNo: Int,
    @SerializedName("gboard_no") val gboardNo: Int,
    @SerializedName("original_file_name") val originalFileName: String?,
    @SerializedName("stored_file_name") val storedFileName: String?,
    @SerializedName("file_size") val fileSize: String?,
    @SerializedName("create_date") val createDate: String?
)
