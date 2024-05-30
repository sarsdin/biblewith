package jm.preversion.biblewith.rtc.webrtc.sessions

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
     *  주의점: progress Bar의 경우 속성 progress의 값의 범위는 0.0 ~ 1.0f 임. 100단위가 아니라 소수단위.
     *  고로, 이 값이 변하는 범위는 반드시 위의 범위를 벋어나선 안된다. 정상동작을 안할 것이다.
     */
    var progress:MutableStateFlow<Float> = MutableStateFlow(0f)
) {

}