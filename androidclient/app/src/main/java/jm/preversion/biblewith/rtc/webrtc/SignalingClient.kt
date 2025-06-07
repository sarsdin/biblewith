package jm.preversion.biblewith.rtc.webrtc

import android.util.Log
import jm.preversion.biblewith.BuildConfig
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.rtc.RtcCommandDto
import jm.preversion.biblewith.rtc.RtcFm
import jm.preversion.biblewith.rtc.RtcRoomDto
import jm.preversion.biblewith.rtc.RtcUserDto
import com.google.gson.Gson
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

/**
 * WebSocket 메시지에서 사용되는 주요 JSON 필드
 *  - command, signalingCommand
 *  - peerId, peerIdOf, sdp
 *  - id, nick, groupId
 *  - roomList, roomInfo, makerId, roomId
 *  - requestL, usersInfo
 *  - title, size, pwd
 *  - sessionState
 */
class SignalingClient(val groupVm: GroupVm) {

    val tagName = "[${this.javaClass.simpleName}]"
    private val logger by taggedLogger("Call:SignalingClient")
    private val gson = Gson()

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
    private val _signalingCommandFlow = MutableSharedFlow<Pair<SignalingCommand, RtcCommandDto>>()
    val signalingCommandFlow: SharedFlow<Pair<SignalingCommand, RtcCommandDto>> = _signalingCommandFlow



    /**
     * 방목록 관련 상태 변수들.
     */
    private val _roomList = MutableStateFlow(emptyList<RtcRoomDto>())
    val roomList: StateFlow<List<RtcRoomDto>> = _roomList

    private val _currentScreen = MutableStateFlow(RtcFm.ScreenState.ROOM_LIST)
    val currentScreen: StateFlow<RtcFm.ScreenState> = _currentScreen/*.asStateFlow()*/

    /**
     * 현재 접속한 방에 대한 정보 - 방만들기 , 방접속시 업데이트됨
     */
    var _접속한방정보 = MutableStateFlow<RtcRoomDto?>(null)

    /**
     * RtcVm에서 관찰중임 -> CustomDialogAtRoomClick 다이얼로그에서 받아씀.
     */
    var _방참가시접속인원목록 = MutableStateFlow(emptyList<RtcUserDto>())
    val 방참가시접속인원목록: StateFlow<List<RtcUserDto>> = _방참가시접속인원목록

    var _전달받은명령상태값 = MutableStateFlow<RtcCommandDto?>(null)

    /**
     * 방에 참가요청자 명단 - 방장용
     */
    var _방장에게접속요청자목록 = MutableStateFlow(emptyList<RtcUserDto>())



    /**
     *  현재 화면을 변경하는 함수를 추가.
     */
    fun setCurrentScreen(screen: RtcFm.ScreenState) {
        _currentScreen.value = screen
    }





    init {
        //이 객체가 처음 생성될때(앱이 페이지들올때)마다 실행하여 시그널링 서버에 클라이언트 정보 중복확인하고 등록해야함.
        // (clients map에 등록)
        val jOut = RtcCommandDto(
            command = "ws_init",
            id = MyApp.userInfo.user_email,
            nick = MyApp.userInfo.user_nick,
            groupId = groupVm.groupInfo.get("group_no").asInt
        )
        logger.w { "[sendCommand Init] $jOut" }
        ws.send(gson.toJson(jOut))

        //TODO 이후에 할일: 방먼저 만들고 만든 방들을 초기에 RTCFM PAGE로 접속시 리사이클러뷰로 로드해올수 있도록 목록을 소켓으로부터 받아야함.

    }



    /**
     * 시그널링 서버로 명령어와 그에 필요한 정보를 문자열로 보냄.
     */
    fun sendCommand(standardCommand: StandardCommand, jOut: RtcCommandDto) {
        logger.w { "sendCommand() jOut: $standardCommand, $jOut" }
//        val jOut = JsonObject()
//        jOut.addProperty("command", "signalingCommand")
//        jOut.addProperty("signalingCommand", "$standardCommand")

        ws.send(gson.toJson(jOut))
    }


    /**
     * 시그널링 서버로 명령어와 그에 필요한 정보를 문자열로 보냄.
     */
    fun sendCommand(signalingCommand: SignalingCommand, peerId:String, message: String, peerIdOf:String = "") {
        if(signalingCommand != SignalingCommand.ICE){
            logger.d { "sendCommand() SignalingCommand: $signalingCommand" }
        }
        val jOut = RtcCommandDto(
            command = "signalingCommand",
            signalingCommand = signalingCommand.name,
            peerId = peerId,
            peerIdOf = peerIdOf,
            sdp = message
        )
        ws.send(gson.toJson(jOut))
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
                // websocket 메시지를 DTO 로 변환
                val jin = gson.fromJson(text, RtcCommandDto::class.java)
                val command = jin.command

                when(command){
                    "signalingCommand" -> {
                        val signalingCommand: String = jin.signalingCommand ?: return
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

                            signalingCommand.startsWith(SignalingCommand.CLOSE.toString(), true) ->{
                                Log.e(tagName, "onMessage() CLOSE(특정 peer 접속종료): $text")
                                handleSignalingCommand(SignalingCommand.CLOSE, jin)
                            }

                        }
                    }

                    StandardCommand.방목록전달.name -> {
                        Log.e(tagName, "방목록전달 jin: $jin")
                        signalingScope.launch {
                            val roomList = jin.roomList ?: emptyList()
                            _roomList.emit(roomList)
                        }
                    }
                    StandardCommand.방만들기.name -> {
                        Log.e(tagName, "방만들기 jin: $jin")

                        signalingScope.launch {
                            val roomList = jin.roomList ?: emptyList()
                            Log.e(tagName, "방만들기 roomList: $roomList")
                            _roomList.emit(roomList)

                            if (jin.makerId == MyApp.userInfo.user_email) {
                                setCurrentScreen(RtcFm.ScreenState.VIDEO_CALL_SCREEN)
                                jin.roomInfo?.let { _접속한방정보.emit(it) }
                            }
                        }
                    }
                    StandardCommand.방접속요청.name -> {
                        Log.e(tagName, "방접속요청 jin: $jin")
                        signalingScope.launch {
                            _방장에게접속요청자목록.emit(jin.requestL ?: emptyList())
                        }
                    }
                    StandardCommand.방참가수락.name -> {
                        Log.e(tagName, "방참가수락 jin: $jin")
                        val usersInfo = jin.usersInfo ?: emptyList()
                        signalingScope.launch {
                            _방참가시접속인원목록.emit(usersInfo)

                            //수락받으면 인원수 초과등 검사를 위해 다시 서버로 보내줌.
                            sendCommand(
                                StandardCommand.방접속,
                                RtcCommandDto(
                                    command = "방접속",
                                    makerId = jin.makerId
                                )
                            )
                        }
                    }
                    StandardCommand.방접속.name -> {
                        Log.e(tagName, "방접속 jin: $jin")
                        _접속한방정보.value = jin.roomInfo
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
                    StandardCommand.방종료.name -> {
                        Log.e(tagName, "방종료 jin: $jin")
                        val roomId = jin.roomId
                        val verifiedArray = _roomList.value.filterNot { it.roomId == roomId }
                        _roomList.value = verifiedArray
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
    private fun handleStateMessage(message: RtcCommandDto) {
        val mType =  message.signalingCommand
        val state = message.sessionState
        Log.w(tagName, "handleStateMessage: $mType, $state")
        state?.let {
            _sessionStateFlow.value = WebRTCSessionState.valueOf(it)
        }
    }

    /**
     * 서버로부터 받은 명령타입의 상태값이 'STATE'이외(OFFER ANSWER ICE) 값이라면 실행하는 메소드.
     * 세션 상태 Flow 값을 업데이트함.
     */
    private fun handleSignalingCommand(command: SignalingCommand, message: RtcCommandDto) {
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
    방접속요청,
    방참가,
    방접속,
    방목록전달,
    방종료,
    접속해제,
    방참가수락

}
