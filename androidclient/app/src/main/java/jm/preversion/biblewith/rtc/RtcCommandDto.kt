package jm.preversion.biblewith.rtc

data class RtcCommandDto(
    val command: String,
    val signalingCommand: String? = null,
    val peerId: String? = null,
    val peerIdOf: String? = null,
    val sdp: String? = null,
    val roomId: String? = null,
    val roomInfo: RtcRoomDto? = null,
    val roomList: List<RtcRoomDto>? = null,
    val requestL: List<RtcUserDto>? = null,
    val usersInfo: List<RtcUserDto>? = null,
    val makerId: String? = null,
    val sessionState: String? = null,
    val groupId: Int? = null,
    val title: String? = null,
    val size: String? = null,
    val pwd: String? = null,
    val id: String? = null,
    val nick: String? = null
)
