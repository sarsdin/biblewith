package com.example.androidclient.rtc

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RtcVm : ViewModel() {

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