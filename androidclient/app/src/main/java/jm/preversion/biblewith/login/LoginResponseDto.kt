package jm.preversion.biblewith.login

import com.google.gson.annotations.SerializedName

data class LoginResponseDto(
    @SerializedName("result") val result: String?,
    @SerializedName("msg") val msg: String?
)
