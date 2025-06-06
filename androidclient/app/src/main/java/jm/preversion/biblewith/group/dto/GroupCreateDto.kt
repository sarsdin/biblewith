package jm.preversion.biblewith.group.dto

import com.google.gson.annotations.SerializedName

data class GroupCreateDto (

    @SerializedName("msg") // JSON 필드 이름과 변수 이름이 다를 경우 사용
    val status: String,    // "msg" 대신 "status"라는 변수명을 사용하고 싶을 경우

    @SerializedName("groupId")
    val groupId: Int?,     // groupId는 없을 수도 있으므로 nullable로 선언

//    @SerializedName("message")
//    val message: String?   // message도 없을 수도 있으므로 nullable로 선언










)