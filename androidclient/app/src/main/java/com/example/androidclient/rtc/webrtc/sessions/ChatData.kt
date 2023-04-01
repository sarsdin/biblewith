package com.example.androidclient.rtc.webrtc.sessions

data class ChatData(
    var message:String = "",
    var file:ByteArray? = null,
    var type:String = "",
    var userId:String = "",
    var nick:String = "",
    var roomId:String = "",
    var etc:String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatData

        if (message != other.message) return false
        if (file != null) {
            if (other.file == null) return false
            if (!file.contentEquals(other.file)) return false
        } else if (other.file != null) return false
        if (type != other.type) return false
        if (userId != other.userId) return false
        if (nick != other.nick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + (file?.contentHashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + nick.hashCode()
        return result
    }


}