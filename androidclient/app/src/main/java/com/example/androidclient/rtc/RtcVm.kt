package com.example.androidclient.rtc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidclient.group.GroupVm
import com.example.androidclient.rtc.webrtc.sessions.ChatData
import com.example.androidclient.rtc.webrtc.sessions.WebRtcSessionManagerImpl
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RtcVm : ViewModel() {

    lateinit var sessionManager: WebRtcSessionManagerImpl
    lateinit var groupVm: GroupVm



    val 접속한방정보읽기: JsonObject
        get() = sessionManager.signalingClient._접속한방정보.value

    val 접속한방정보: StateFlow<JsonObject>
        get() = sessionManager.signalingClient._접속한방정보

    val 방참가시접속인원목록: StateFlow<JsonArray>
        get() = sessionManager.signalingClient._방참가시접속인원목록

    val 전달받은명령상태값: StateFlow<JsonObject>
        get() = sessionManager.signalingClient._전달받은명령상태값

    val 방장에게접속요청자목록: StateFlow<JsonArray>
        get() = sessionManager.signalingClient._방장에게접속요청자목록

    val 방장에게접속요청자목록size :Int
        get() = sessionManager.signalingClient._방장에게접속요청자목록.value.size()




    val chatMessages: StateFlow<List<ChatData>>
        get() = sessionManager._chatMessages

    fun addChatMessage(chatData : ChatData){
        viewModelScope.launch {
//            sessionManager._chatMessages.emit(
//                (sessionManager._chatMessages.replayCache.firstOrNull()
//                    ?: emptyList<ChatData>() ) + chatData
//            )
            sessionManager._chatMessages.emit(
                (sessionManager._chatMessages.value + chatData)
            )
//            sessionManager._chatMessages.value = sessionManager._chatMessages.value + chatData
            Log.e("RtcVm", "ChatData: ${sessionManager._chatMessages.value.size}")
        }
    }


}






//    private val _roomList = MutableStateFlow(emptyList<String>())
//    val roomList: StateFlow<List<String>> = _roomList.asStateFlow()
//
//    private val _currentScreen = MutableStateFlow(RtcFm.ScreenState.ROOM_LIST)
//    val currentScreen: StateFlow<RtcFm.ScreenState> = _currentScreen.asStateFlow()
//
//    // 웹소켓으로부터 받은 방 목록을 업데이트하는 함수를 추가합니다.
//    fun updateRoomList(newRoomList: List<String>) {
//        _roomList.value = newRoomList
//    }
//
//    // 현재 화면을 변경하는 함수를 추가합니다.
//    fun setCurrentScreen(screen: RtcFm.ScreenState) {
//        _currentScreen.value = screen
//    }
