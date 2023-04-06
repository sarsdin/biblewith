package com.example.androidclient.rtc.webrtc.sessions

import kotlinx.coroutines.flow.MutableStateFlow

data class ChatData(
    var message:String = "",
    var file:ByteArray? = null,
    var type:String = "",
    var userId:String = "",
    var nick:String = "",
    var roomId:String = "",
    var etc:String = "",
//    var progress:Float = 0f,
    /**
     *  ChatItem 컴포넌트에서 사용. 파일을 보낼때, 보낸 peer의 수에 따라 progress의 Float값이 변하고,
     *  그에 따라 이 변수를 구독하는 쪽에서 상태값의 변화에 따른 동작을 수행할 수 있다.
     */
    var progress:MutableStateFlow<Float> = MutableStateFlow(0f)
) {

}