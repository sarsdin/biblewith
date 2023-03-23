package com.example.androidclient.rtc

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import com.example.androidclient.rtc.webrtc.sessions.WebRtcSessionManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RtcVm : ViewModel() {

    lateinit var sessionManager: WebRtcSessionManager

    //    private val 방접속시도시접속인원목록 = MutableStateFlow(emptyList<String>())
    val 방접속시도시접속인원목록: StateFlow<JsonArray>
        get() = sessionManager.signalingClient._방접속시도시접속인원목록


    private val _roomList = MutableStateFlow(emptyList<String>())
    val roomList: StateFlow<List<String>> = _roomList.asStateFlow()

    private val _currentScreen = MutableStateFlow(RtcFm.ScreenState.ROOM_LIST)
    val currentScreen: StateFlow<RtcFm.ScreenState> = _currentScreen.asStateFlow()

    // 웹소켓으로부터 받은 방 목록을 업데이트하는 함수를 추가합니다.
    fun updateRoomList(newRoomList: List<String>) {
        _roomList.value = newRoomList
    }

    // 현재 화면을 변경하는 함수를 추가합니다.
    fun setCurrentScreen(screen: RtcFm.ScreenState) {
        _currentScreen.value = screen
    }


}