package com.example.androidclient.rtc.webrtc
import android.util.Log
import com.example.androidclient.BuildConfig
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class SignalingClient {

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
    private val _signalingCommandFlow = MutableSharedFlow<Pair<SignalingCommand, String>>()
    val signalingCommandFlow: SharedFlow<Pair<SignalingCommand, String>> = _signalingCommandFlow


    /**
     * 시그널링 서버로 명령어와 그에 필요한 정보를 문자열로 보냄.
     */
    fun sendCommand(signalingCommand: SignalingCommand, message: String) {
        logger.d { "[sendCommand] $signalingCommand" }
        ws.send("$signalingCommand $message")
    }



    /**
     *  웹소켓 리스너 구현부 클래스.
     *  시그널링 서버에 request 요청하고, 거기에서 오는 메시지 등을 받았을때 동작하는 리스너.
     */
    private inner class SignalingWebSocketListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            JsonObject()
            val jin = JsonParser.parseString(text).asJsonObject
            val signalingCommand = jin["SignalingCommand"].asString

            // 각 응답의 내용에 따른 메소드를 호출함.
            when {
                //text의 앞글자가 STATE 일때 실행.
                //서버로부터 'STATE Impossible' << 서버 접속 peer가 2명 미만인 상태.
                //'STATE Ready' << 서버 접속 peer가 2명이상일시 웹소켓을 통해 전달되어옴.
                //'STATE Creating'  << 서버에서 OFFER 명령을 받으면
                //'STATE Active'  << 서버에서 ANSWER 명령을 받으면
                //'STATE ICE'  << 서버에서 ICE 명령을 받으면
                text.startsWith(SignalingCommand.STATE.toString(), true) ->
                    handleStateMessage(text)


                //text의 앞글자가 OFFER, ANSWER, ICE 일때 실행.
                //WebRtcSessionManagerImpl의 init{} 에서 SignalingCommand의 값을 collect하는 코루틴이 존재.
                //거기서 handleOffer handleAnswer handleIce 등의 명령을 실행함.
                text.startsWith(SignalingCommand.OFFER.toString(), true) ->
                    handleSignalingCommand(SignalingCommand.OFFER, text)
                text.startsWith(SignalingCommand.ANSWER.toString(), true) ->
                    handleSignalingCommand(SignalingCommand.ANSWER, text)
                text.startsWith(SignalingCommand.ICE.toString(), true) ->
                    handleSignalingCommand(SignalingCommand.ICE, text)
            }
        }
    }

    /**
     * 서버로부터 받은 명령타입의 상태값이 'STATE'라면 실행하는 메소드.
     * 세션 상태 Flow 값을 업데이트함.
     * 서버로부터 'STATE Ready' 값을 받으면 _sessionStateFlow의 상태값을 업데이트하고
     * 그 변화를 감지한 Ui 컴포지션은 조건에 따라 다음 작업을 진행한다.
     */
    private fun handleStateMessage(message: String) {
        Log.w(tagName, "handleStateMessage: $message")
        val state = getSeparatedMessage(message) //message에 공백이 포함되어 있을 수 있기 때문에 텍스트 전처리함.
        _sessionStateFlow.value = WebRTCSessionState.valueOf(state)
    }

    /**
     * 서버로부터 받은 명령타입의 상태값이 'STATE'이외(OFFER ANSWER ICE) 값이라면 실행하는 메소드.
     * 세션 상태 Flow 값을 업데이트함.
     */
    private fun handleSignalingCommand(command: SignalingCommand, text: String) {
        val value = getSeparatedMessage(text)
        logger.w { "[emit!] received SignalingCommand: $command, 값(value): $value" }
        signalingScope.launch {
            //시그널링 서버로부터 받은 값에 따라 현재 WebRtc 단계의 상태값을 업데이트함.
            //emit 함으로써 _signalingCommandFlow를 구독하고 있는 모든 곳에 flow를 일으켜 collect실행하게 함.
            //WebRtcSessionManagerImpl의 init에서 하나의 코루틴내에서 구독중임.
            _signalingCommandFlow.emit(command to value)
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
        _sessionStateFlow.value = WebRTCSessionState.Offline
        signalingScope.cancel()
        ws.cancel()
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
    ICE // to send and receive ice candidates
}
