package com.example.androidclient.rtc.webrtc
import android.util.Log
import com.example.androidclient.BuildConfig
import com.example.androidclient.MyApp
import com.example.androidclient.group.GroupVm
import com.example.androidclient.rtc.RtcFm
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*

class SignalingClient(val groupVm: GroupVm) {

    val tagName = "[${this.javaClass.simpleName}]"
    private val logger by taggedLogger("Call:SignalingClient")

    // SupervisorJob() 은 코루틴을 계층적으로 사용하기 위한 기능. 이것을 더해서 범위를 설정하면 곧 최상위 작업이 되고, 하위의 코루틴 작업의 취소
    // 는 이 작업의 취소에는 영향을 미치지 않음. 다만, 하위 코루틴의 취소에 대한 예외처리는 CoroutineExceptionHandler 통해 가능함.
    private val signalingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = OkHttpClient()
    private val request = Request
        .Builder()
        .url(BuildConfig.SIGNALING_SERVER_IP_ADDRESS) //시그널링 서버 아이피 등록함. BuildConfig는 build.gradle.ks(module)에서 local.properties로 등록가능.
        .build()

    // opening web socket with signaling server
    //  웹소켓을 이용해 시그널링 서버로 연결함.
    // 웹소켓 객체 생성시, request와 함께 이 웹소켓의 응답에 대한 행동을 작성한 리스너 클래스도 같이 등록함.
    private val ws = client.newWebSocket(request, SignalingWebSocketListener())

    // session flow to send information about the session state to the subscribers
    // 세션의 상태에 관한 정보를 호스트에 연결된 구독자에 전달함.
    private val _sessionStateFlow = MutableStateFlow(WebRTCSessionState.Offline)
    val sessionStateFlow: StateFlow<WebRTCSessionState> = _sessionStateFlow

    // signaling commands to send commands to value pairs to the subscribers
    // 시그널링 서버로부터 온 message text에 따른 SignalingCommand의 값을 변화시켜(flow발생)
    // 이 Flow를 collect(구독)하고 있는 구독자가 collect를 실행하게함.
    // 웹소켓의 리스너의 onMessage에 따라 해당하는 상태메시지의 handleSignalingCommand()가 실행되고 이 값이 변경됨.
    private val _signalingCommandFlow = MutableSharedFlow<Pair<SignalingCommand, JsonObject>>()
    val signalingCommandFlow: SharedFlow<Pair<SignalingCommand, JsonObject>> = _signalingCommandFlow



    /**
     * 방목록 관련 상태 변수들.
     */
    private val _roomList = MutableStateFlow(JsonArray())
    val roomList: StateFlow<JsonArray> = _roomList

    private val _currentScreen = MutableStateFlow(RtcFm.ScreenState.ROOM_LIST)
    val currentScreen: StateFlow<RtcFm.ScreenState> = _currentScreen/*.asStateFlow()*/

    /**
     * RtcVm에서 관찰중임 -> CustomDialogAtRoomClick 다이얼로그에서 받아씀.
     */
    var _방접속시도시접속인원목록 = MutableStateFlow(JsonArray())
    val 방접속시도시접속인원목록: StateFlow<JsonArray> = _방접속시도시접속인원목록

    /**
     * 웹소켓으로부터 받은 방 목록을 업데이트하는 함수를 추가합니다.
     */
    fun updateRoomList(newRoomList: JsonArray) {
        _roomList.value = newRoomList
    }

    /**
     *  현재 화면을 변경하는 함수를 추가합니다.
     */
    fun setCurrentScreen(screen: RtcFm.ScreenState) {
        _currentScreen.value = screen
    }





    init {
        //이 객체가 처음 생성될때(앱이 페이지들올때)마다 실행하여 시그널링 서버에 클라이언트 정보 중복확인하고 등록해야함.
        // (clients map에 등록)
        val jOut = JsonObject()
        jOut.addProperty("command", "ws_init")
        jOut.addProperty("id", MyApp.userInfo.user_email)
        jOut.addProperty("nick", MyApp.userInfo.user_nick)
        jOut.addProperty("groupId", groupVm.groupInfo.get("group_no").asInt)
        logger.w { "[sendCommand Init] $jOut" }
        ws.send("$jOut")

        //TODO 이후에 할일: 방먼저 만들고 만든 방들을 초기에 RTCFM PAGE로 접속시 리사이클러뷰로 로드해올수 있도록 목록을 소켓으로부터 받아야함.

    }



    /**
     * 시그널링 서버로 명령어와 그에 필요한 정보를 문자열로 보냄.
     */
    fun sendCommand(standardCommand: StandardCommand, jOut: JsonObject) {
        logger.w { "sendCommand() jOut: $standardCommand, $jOut" }
//        val jOut = JsonObject()
//        jOut.addProperty("command", "signalingCommand")
//        jOut.addProperty("signalingCommand", "$standardCommand")

        ws.send(jOut.toString())
    }


    /**
     * 시그널링 서버로 명령어와 그에 필요한 정보를 문자열로 보냄.
     */
    fun sendCommand(signalingCommand: SignalingCommand, peerId:String, message: String, peerIdOf:String = "") {
        if(signalingCommand != SignalingCommand.ICE){
            logger.d { "sendCommand() SignalingCommand: $signalingCommand" }
        }
        val jOut = JsonObject()
        jOut.addProperty("command", "signalingCommand")
        jOut.addProperty("signalingCommand", "$signalingCommand")
        //OFFER, ANSWER의 경우 SDP. ICE의 경우 iceCandidate.sdpMid, sdpMLineIndex, sdp 3가지 정보.
        jOut.addProperty("peerId", peerId)
        jOut.addProperty("peerIdOf", peerIdOf) // sendAnswer()에서 사용됨. 다른데서는 사용x
        jOut.addProperty("sdp", message)
        ws.send(jOut.toString())
//        ws.send("$signalingCommand $message")
    }



    /**
     *  웹소켓 리스너 구현부 클래스.
     *  시그널링 서버에 request 요청하고, 거기에서 오는 메시지 등을 받았을때 동작하는 리스너.
     */
    private inner class SignalingWebSocketListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
//            Log.e(tagName, "onMessage(): $text")

            try {
                //websocket으로 들어오는 메시지를 json object로 해석.
                val jin = JsonParser.parseString(text).asJsonObject
                val command = jin["command"].asString

                when(command){
                    "signalingCommand" -> {
                        val signalingCommand: String = jin.get("signalingCommand").asString
                        // 각 응답의 내용에 따른 메소드를 호출함.
                        when {
                            //text의 앞글자가 STATE 일때 실행.
                            //서버로부터 'STATE Impossible' << 서버 접속 peer가 2명 미만인 상태.
                            //'STATE Ready' << 서버 접속 peer가 2명이상일시 웹소켓을 통해 전달되어옴.
                            //'STATE Creating'  << 서버에서 OFFER 명령을 받으면
                            //'STATE Active'  << 서버에서 ANSWER 명령을 받으면
                            //'STATE ICE'  << 서버에서 ICE 명령을 받으면
                            signalingCommand.startsWith(SignalingCommand.STATE.toString(), true) ->
                                handleStateMessage(jin)


                            //text의 앞글자가 OFFER, ANSWER, ICE 일때 실행.
                            //WebRtcSessionManagerImpl의 init{} 에서 SignalingCommand의 값을 collect하는 코루틴이 존재.
                            //거기서 handleOffer handleAnswer handleIce 등의 명령을 실행함.
                            signalingCommand.startsWith(SignalingCommand.OFFER.toString(), true) ->{
                                Log.e(tagName, "onMessage() OFFER: $text")
                                handleSignalingCommand(SignalingCommand.OFFER, jin)
                            }
                            signalingCommand.startsWith(SignalingCommand.ANSWER.toString(), true) ->{
                                Log.e(tagName, "onMessage() ANSWER: $text")
                                handleSignalingCommand(SignalingCommand.ANSWER, jin)
                            }

                            // Observer.onIceCandidate()시 콜백을 실행하는데, 그 콜백에서 소켓으로 ice관련 명령을 보냄.
                            // onIceCandidateRequest <<< 이것임.
                            signalingCommand.startsWith(SignalingCommand.ICE.toString(), true) ->
                                handleSignalingCommand(SignalingCommand.ICE, jin)

                        }
                    }
                    "방목록전달" -> {
                        Log.e(tagName, "방목록전달 jin: $jin")
                        val roomList = jin["roomList"].asJsonArray
                        updateRoomList(roomList)
                    }
                    "방만들기" -> {
                        //map을 tojson으로 변환한건데 이게 JsonObject로 변환된건지 잘모르겠네.
                        Log.e(tagName, "방만들기 jin: $jin")
                        val roomList = jin["roomList"].asJsonArray
                        Log.e(tagName, "방만들기 roomList: $roomList")
                        updateRoomList(roomList)
                        // todo 방만들기시에 방장이 방에 바로 접속할 수 있도록 하는 코드를 짜야함.
                        // 서버로부터 방을 만든 아이디를 전달받아 비교하여, 방장본인이 만든 방이면 방장을 바로 비디오화면으로 전환함.
                        if(jin["makerId"].asString == MyApp.userInfo.user_email){
                            setCurrentScreen(RtcFm.ScreenState.VIDEO_CALL_SCREEN)
                        }
                    }
                    "방접속시도" -> {
                        Log.e(tagName, "방접속시도 jin: $jin")
                        val userIds = jin["userIds"].asJsonArray
                        _방접속시도시접속인원목록.value = userIds
                    }
                    "방접속" -> {
                        Log.e(tagName, "방접속 jin: $jin")
//                        val userIds = jin["userIds"].asJsonArray
                        // 소켓으로부터 이 응답을 받으면, 이 클라이언트의 화면을 VIDEO_CALL_SCREEN 으로 전환.
                        // 거기서 _방접속시도시접속인원목록.value 의 값을 이용해 원격 비디오 렌더링뷰를 셋팅해야함.
                        setCurrentScreen(RtcFm.ScreenState.VIDEO_CALL_SCREEN)

                    }
                    "피어연결종료신호" -> {
                        // todo 현재 방에서 연결된 다른 peer가 연결을 종료하거나 끊켰을때, 웹소켓의 sessionClose()
                        //  함수를 통하여 어떤 명령을 받으면, 이곳의 WebRtcSessionManagerImpl에서 관리되고 있는
                        //  peerConnections Map에서 (종료된 peerId를 전달받아) peerConnection객체를 제거해줘야함.
                        Log.e(tagName, "피어연결종료신호 jin: $jin")
//                        val roomList = jin["roomList"].asJsonArray
//                        updateRoomList(roomList)
                    }
                    "방종료" -> {
                        Log.e(tagName, "방종료 jin: $jin")
                        val roomId = jin["roomId"].asString
                        val verifiedArray = _roomList.value.run {
                            remove(find {
                                it.asJsonObject["roomId"].asString == roomId
                            })
                        }
                        // todo  emit 안하고도 상태값의 변화가 감지될려나? 방이 리스트에서 제대로 종료안되면 확인해봐야함.
//                        _roomList.emit(verifiedArray)
                    }

                }

            } catch(e: Exception){
                Log.e(tagName, "onMessage() Exception: $e")
            }

        }


        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.e(tagName, "onOpen() response handshake: ${response.handshake?.javaClass?.simpleName}")
            Log.e(tagName, "onOpen() response headers: ${response.headers}")
        }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.e(tagName, "onClosed() reason: $code $reason")
            // todo 서버로의 웹소켓이 이 RTC_FM 안에 있는 동안에는 끊키면 안된다.
            //  혹시, 끊킬때 여기서 웹소켓 객체를 재생성하여, ws객체에 재할당해주는 코드를 작성해보자.

        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.e(tagName, "onClosing() reason: $code $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e(tagName, "onFailure() reason: $t, response: $response")
        }

    }

    /**
     * 서버로부터 받은 명령타입의 상태값이 'STATE'라면 실행하는 메소드.
     * 세션 상태 Flow 값을 업데이트함.
     * 서버로부터 'STATE Ready' 값을 받으면 _sessionStateFlow의 상태값을 업데이트하고
     * 그 변화를 감지한 Ui 컴포지션은 조건에 따라 다음 작업을 진행한다.
     */
    private fun handleStateMessage(message: JsonObject) {
        val mType =  message["signalingCommand"].asString
        val state = message["sessionState"].asString
        Log.w(tagName, "handleStateMessage: $mType, $state")
//        val state = getSeparatedMessage(message) //message에 공백이 포함되어 있을 수 있기 때문에 텍스트 전처리함.
        _sessionStateFlow.value = WebRTCSessionState.valueOf(state)
    }

    /**
     * 서버로부터 받은 명령타입의 상태값이 'STATE'이외(OFFER ANSWER ICE) 값이라면 실행하는 메소드.
     * 세션 상태 Flow 값을 업데이트함.
     */
    private fun handleSignalingCommand(command: SignalingCommand, message: JsonObject) {
//        val mType =  message["signalingCommand"].asString
//        val value = getSeparatedMessage(text)
//        val sdp = message["sdp"].asString
//        logger.w { "handleSignalingCommand() SignalingCommand 받음: $command"/*, 값(value): $sdp"*/ }
        signalingScope.launch {
            //시그널링 서버로부터 받은 값에 따라 현재 WebRtc 단계의 상태값을 업데이트함.
            //emit 함으로써 _signalingCommandFlow를 구독하고 있는 모든 곳에 flow를 일으켜 collect실행하게 함.
            //WebRtcSessionManagerImpl의 init에서 하나의 코루틴내에서 구독중임.
            _signalingCommandFlow.emit(command to message)
        }
    }


    /**
     * 텍스트를 ' ' 스페이스 딜리미터로 짜르고, 짜르고 난뒤의 바로 그 첫번째 요소를 반환.
     * 여기서는 SignalingCommand를 제외하고 남은 문자열을 반환.
     * 거의 SDP 정보에 관한 문자열임.
     */
    private fun getSeparatedMessage(text: String) = text.substringAfter(' ')


    //웹소켓 연결을 끊어야 할때 사용. 세션 플로우 상태의 값을 Offline이라고 변경하고
    fun dispose() {
        Log.e(tagName, "Session disconnect() 9")
        _sessionStateFlow.value = WebRTCSessionState.Offline
        Log.e(tagName, "Session disconnect() 10")
        signalingScope.cancel()
        Log.e(tagName, "Session disconnect() 11")
        ws.cancel()
        Log.e(tagName, "Session disconnect() 12")
    }
}


/**
 * VideoCallScreen()에서 처음 sessionManager.onSessionScreenReady() 가 실행되고,
 *
 */
enum class WebRTCSessionState {
    Active, // Offer and Answer messages has been sent. 오퍼와 엔서 메시지가 보내졌을때.
    Creating, // Creating session, offer has been sent. 오퍼가 보내지고 세션 생성중일때.
    Ready, // Both clients available and ready to initiate session. 세션이 완성되고 초기화(접속) 가능할때.
    Impossible, // We have less than two clients connected to the server. 시그널링 서버에 접속한 인원이 2명 미만일때.
    Offline // unable to connect signaling server.  시그널링 서버에 연결이 안될때
}

enum class SignalingCommand {
    STATE, // Command for WebRTCSessionState
    OFFER, // to send or receive offer
    ANSWER, // to send or receive answer
    ICE, // to send and receive ice candidates
    CLOSE // peer가 종료되거나 연결 끊킴.
}
enum class StandardCommand {
    방만들기,
    방접속,
    방목록전달,
    방접속시도,
    접속해제
}
