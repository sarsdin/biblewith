package com.example.androidclient.rtc.webrtc.sessions
import android.app.Activity
import android.util.Log

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.window.layout.WindowMetricsCalculator
import com.example.androidclient.MyApp
import com.example.androidclient.home.MainActivity
import com.example.androidclient.rtc.MediaProjectionService
import com.example.androidclient.rtc.RtcFm
import com.example.androidclient.rtc.webrtc.SignalingClient
import com.example.androidclient.rtc.webrtc.SignalingCommand
import com.example.androidclient.rtc.webrtc.audio.AudioHandler
import com.example.androidclient.rtc.webrtc.audio.AudioSwitchHandler
import com.example.androidclient.rtc.webrtc.peer.StreamPeerConnection
import com.example.androidclient.rtc.webrtc.peer.StreamPeerConnectionFactory
import com.example.androidclient.rtc.webrtc.peer.StreamPeerType
import com.google.gson.JsonParser
import io.getstream.log.taggedLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.*
import java.util.UUID

private const val ICE_SEPARATOR = '$'

/**
 * MainActivity에서 등록한  CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager)의 값을 불러올 수 있는
 * 변수.
 * */
val LocalWebRtcSessionManager: ProvidableCompositionLocal<WebRtcSessionManager> =
    staticCompositionLocalOf { error("WebRtcSessionManager was not initialized!") }



/**
 * 필요한 생성자로 컨텍스트, 시그널링 클라이언트, peer connection Factory 등의 객체가 필요함.
 * */
class WebRtcSessionManagerImpl(
    protected val context: Context,
    override val signalingClient: SignalingClient,
    override val peerConnectionFactory: StreamPeerConnectionFactory,
    val rtcFm: RtcFm
) : WebRtcSessionManager {


    val tagName = "[${this.javaClass.simpleName}]"
    private val logger by taggedLogger("Call:LocalWebRtcSessionManager")

    // SupervisorJob() 은 코루틴을 계층적으로 사용하기 위한 기능. 이것을 더해서 범위를 설정하면 곧 최상위 작업이 되고, 하위의 코루틴 작업의 취소
    // 는 이 작업의 취소에는 영향을 미치지 않음. 다만, 하위 코루틴의 취소에 대한 예외처리는 CoroutineExceptionHandler 통해 가능함.
    private val sessionManagerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // used to send local video track to the fragment(ui를 말하는 듯)
    //WebRtcSessionManager의 멤버변수를 오버라이드함.
    private val _localVideoTrackFlow = MutableSharedFlow<VideoTrack>()
    override val localVideoTrackFlow: SharedFlow<VideoTrack> = _localVideoTrackFlow

    // used to send remote video track to the sender
    // 나의 비디오를 다른 원격자들에 보낸다는 건지, 원격자들의 비디오를 내 ui에 보낸다는 건지 확인필요.
//    private val _remoteVideoTrackFlow = MutableSharedFlow<VideoTrack>()
//    override val remoteVideoTrackFlow: SharedFlow<VideoTrack> = _remoteVideoTrackFlow
//    val _remoteVideoTracks = MutableStateFlow(mutableMapOf<String, VideoTrack>())
//    override val remoteVideoTracks: MutableStateFlow<MutableMap<String, VideoTrack>> = _remoteVideoTracks
//    val _remoteVideoTracks = MutableStateFlow(emptyList<VideoTrack>())
//    override val remoteVideoTracks: MutableStateFlow<List<VideoTrack>> = _remoteVideoTracks

    val _remoteVideoTracks = MutableSharedFlow<List<VideoTrack>>(replay = 1)
    override val remoteVideoTracks: SharedFlow<List<VideoTrack>> = _remoteVideoTracks.asSharedFlow()

    //rtc 채팅목록
    val _chatMessages = MutableStateFlow<List<ChatData>>(mutableListOf<ChatData>())



    // declaring video constraints and setting OfferToReceiveVideo to true.
    // this step is mandatory to create valid offer and answer.
    // 원격에서 비디오를 받기 위한 오퍼를 위해 (표준?)미디어 제약사항을 설정함.
    // 필수적인 행위임. 오퍼,엔서를 정확히 생성하기 위해.
    private val mediaConstraints = MediaConstraints().apply {
        mandatory.addAll(
            listOf(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"),
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
            )
        )
    }

    // getting front camera.
    // 전면 카메라를 찾아서 가져오는 절차.
    private val cameraManager by lazy { context.getSystemService<CameraManager>() }  //카메라 메니저 호출해서 가져옴.
    private val videoCapturer: VideoCapturer by lazy{ buildCameraCapturer() }
    private val cameraEnumerator: Camera2Enumerator by lazy { //
        Camera2Enumerator(context)
    }

    /**
     * 카메라가 지원하는 해상도를 담고있는 객체를 받아옴.
     * 해상도를
     */
    private val resolution: CameraEnumerationAndroid.CaptureFormat
        get() {
            val frontCamera = cameraEnumerator.deviceNames.first { cameraName ->
                cameraEnumerator.isFrontFacing(cameraName)
            }
            val supportedFormats = cameraEnumerator.getSupportedFormats(frontCamera) ?: emptyList()
            Log.e(tagName, "supportedFormats: ${supportedFormats}")
            return supportedFormats.firstOrNull {
                (it.width ==  176 || it.width == 320 ||  it.width == 640 || it.width == 720 || it.width == 1280 || it.width == 352)
//                (it.width == 1920 || it.width == 1280 ||  it.width == 720 || it.width == 540 || it.width == 480 || it.width == 360)
//                (it.height == 1920 || it.height == 1280 ||  it.height == 720 || it.height == 540 || it.height == 480 || it.height == 360)
            } ?: error("There is no matched resolution!")
        }


    // we need it to initialize video capturer
    // 비디오 캡처러의 생성을 위한 초기화에 필요함.
    private val surfaceTextureHelper = SurfaceTextureHelper.create(
        "SurfaceTextureHelperThread",
        peerConnectionFactory.eglBaseContext
    )


    // 비디오소스가 필요할때 초기화 할 수 있게함.
    // peerConnectionFactory로부터 만들어서 가져옴
    private val videoSource by lazy {
        // buildCameraCapturer() 메소드로 가져온 videoCapturer객체(Camera2Capturer객체임)를
        peerConnectionFactory.makeVideoSource(videoCapturer.isScreencast).apply {
            //위에서 만들어둔 surfaceTextureHelper 객체를 이용해 초기화함. 초기화 안하면 startCapture시 Exception 발생함.
            //capturerObserver의 구현부는 VideoSource 클래스에 있음.
            //결국,videoCapturer의 초기화는 this객체인 VideoSource를 이용함.
            videoCapturer.initialize(surfaceTextureHelper, context, this.capturerObserver)
            videoCapturer.startCapture(resolution.width, resolution.height, 10)
//            videoCapturer.startCapture(320, 240, 15)
            Log.e(tagName, "makeVideoSource() 실행. videoSource 초기화 완료.")
        }
    }

    /**
     * onSessionScreenReady()실행시 초기화. 즉, Ui sessionState상태값이 Ready일경우 발생.
     * 로컬 비디오 트랙을 팩토리로부터 생성함. 잘보면 videoSource객체를 peer팩토리에 넣어서 VideoTrack를 생성함
     * peerConnectionFactory가 videoSource, VideoTrack 객체 둘다 생성하는 것을 알 수 있다.
     *
     * 다자간 연결에서 remoteVideoTrack의 경우는 각 peer에 해당하는 영상을 받아와야 되고, 해당하는 Track을
     * 각 peerConnection 객체든지 따로 remoteVideoTrack을 모아둔 맵이라든지에 넣어줘야하지만,
     * localVideoTrack은 내 영상 트랙을 여기서 만들고 초기화되면 그것을 타 peerConnection에 여러번 재활용하면됨.
     * */
    val localVideoTrack: VideoTrack by lazy {
        Log.e(tagName, "localVideoTrack 초기화 시작.")
        peerConnectionFactory.makeVideoTrack(
            source = videoSource,
//            trackId = "Video_${MyApp.userInfo.user_email}_${UUID.randomUUID()}"
            trackId = "Video${UUID.randomUUID()}"
        ).run {
            Log.e(tagName, "localVideoTrack 초기화 완료. localVideoTrack: $this")
            this
        }
    }

    /**
     * localVideoTrack가 초기화가 이상하게 잘 안되는 경우 재생성해서 플로우에 넣어줌.
     */
    public fun reCreateLocalVideoTrack(){
        Log.e(tagName, "localVideoTrack 재생성 시작.")
        peerConnectionFactory.makeVideoTrack(
            source = videoSource,
//            trackId = "Video_${MyApp.userInfo.user_email}_${UUID.randomUUID()}"
            trackId = "Video${UUID.randomUUID()}"
        ).run rn@ {
            Log.e(tagName, "localVideoTrack 재생성 완료. localVideoTrack: $this")
//            this
            sessionManagerScope.launch {
                _localVideoTrackFlow.emit(this@rn)
            }
        }
    }


    



    /** Audio properties */
    private val audioHandler: AudioHandler by lazy {
        AudioSwitchHandler(context)
    }

    private val audioManager by lazy {
        context.getSystemService<AudioManager>()
    }

    private val audioConstraints: MediaConstraints by lazy {
        buildAudioConstraints()
    }

    private val audioSource by lazy {
        peerConnectionFactory.makeAudioSource(audioConstraints)
    }

    private val localAudioTrack: AudioTrack by lazy {
        peerConnectionFactory.makeAudioTrack(
            source = audioSource,
            trackId = "Audio${UUID.randomUUID()}"
        )
    }



    /**
     * init{} 안에서 handleOffer()메소드를 통해 할당됨.
     * 서버로부터(타 peer) 받은 Offer정보를 원격 sdp로써 할당한 것.
     */
    private var offer: String? = null


    /**
     * 현재 Room에 연결된 peerConnection 객체들을 갖고있는 Map.
     */
    private val peerConnections = mutableMapOf<String, StreamPeerConnection>()
    fun getPeerConnections():  MutableMap<String, StreamPeerConnection> {
        return peerConnections
    }

    private fun createPeerConnection(peerId: String): StreamPeerConnection {
        val newPeerConnection = peerConnectionFactory.makePeerConnection(
            peerId = peerId,
            coroutineScope = sessionManagerScope,
            configuration = peerConnectionFactory.rtcConfig,
            type = StreamPeerType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
            //Observer.onIceCandidate() 구현부에서 실행할 콜백임.
            // onIceCandidate() <<< 이 메소드는 Offer 시, Answer시 둘다 발동된다. 그래서, offer명령을 서버
            // 에 내리면 ice 명령이 실행되고, answer시에도 마찬가지로 ice 명령이 보내진다.
            // 새로운 iceCandidate가 들어오면 그것을 받아서 시그널링 서버로 ICE 시그널을 보냄.
            onIceCandidateRequest = { iceCandidate, _ ->
                //여기서 peerId는 peerconnection 객체를 생성할때 설정했던, 타겟(상대) peerId 이다.
                // MyApp.userInfo.user_email 나의 peerId는 handelIce()시 상대쪽 peer에서 만들어둔 pc객체를 맵에서 찾기위한 용도.
                signalingClient.sendCommand(
                    SignalingCommand.ICE,  peerId,
                    "${iceCandidate.sdpMid}$ICE_SEPARATOR${iceCandidate.sdpMLineIndex}$ICE_SEPARATOR${iceCandidate.sdp}",
                    MyApp.userInfo.user_email
                )
            },
            //PeerConnection.Observer.onTrack() 구현부에서 실행할 콜백임.
            // 새로운 rtpTransceiver가 들어오면 그것을 받아서 _remoteVideoTrackFlow(원격비디오트랙)에 값으로 넣어줌.
            onVideoTrack = { rtpTransceiver ->
                //리시버에 트랙이 없으면 그대로 메소드 종료
                val track = rtpTransceiver?.receiver?.track() ?: return@makePeerConnection
                //리시브 받은 트랙이 있고, 그것의 종류가 비디오 트랙 종류면 그 비디오 트랙을 받아서
                //원격 비디오 트랙을 저장하는 flow로 보냄.
                if (track.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
                    val videoTrack = track as VideoTrack
//                    videoTracksForRemove[peerId] = videoTrack //나중에 연결종료시 remove용도
                    sessionManagerScope.launch {
//                        _remoteVideoTrackFlow.emit(videoTrack)
//                        _remoteVideoTracks.value[peerId] = videoTrack
//                        _remoteVideoTracks.value = _remoteVideoTracks.value + videoTrack

                        // 원격에서 받아온 videoTrack에 현재 연결된 peerId를 할당해준다.
                        videoTrack.peerInfo["peerId"] = peerId
//                        videoTrack.peerInfo["peerId"] = peerId.substringBefore("@")
                        _remoteVideoTracks.emit(
                        (_remoteVideoTracks.replayCache.firstOrNull()?: emptyList<VideoTrack>() )
                            + videoTrack
                        )
                    }
                }
            },

            //채팅 메시지가 도착했을때 처리.
            onDataMessage = { message, file, type ->
                if (type == "TEXT"){
                    val chatJsonMessage = message?: return@makePeerConnection
                    val chatJin = JsonParser.parseString(chatJsonMessage).asJsonObject
                    sessionManagerScope.launch {
                        _chatMessages.emit(
                        (_chatMessages.replayCache.firstOrNull()?: emptyList<ChatData>())
                            + ChatData(
                                userId = chatJin["peerId"].asString,
                                nick = chatJin["nick"].asString,
                                message = chatJin["message"].asString,
                                type = type
                            )
                        )
                    }

                } else {
                    val chatJsonMessage = message?: return@makePeerConnection
                    Log.e(tagName, "chatJsonMessage: ${chatJsonMessage}")
                    val chatJin = JsonParser.parseString(chatJsonMessage).asJsonObject
                    sessionManagerScope.launch {
                        _chatMessages.emit(
                            (_chatMessages.replayCache.firstOrNull()?: emptyList<ChatData>())
                                    + ChatData(
                                userId = chatJin["peerId"].asString,
                                nick = chatJin["nick"].asString,
                                message = chatJin["message"].asString,
                                type = type,
                                file = file
                            )
                        )
                    }
                }
            }
        )
        peerConnections[peerId] = newPeerConnection
        return newPeerConnection
    }

    //임시객체 - 위에서 받은 비디오트랙을 나중에 제거하기 위해 맵으로 저장해둠.
//    val videoTracksForRemove = mutableMapOf<String, VideoTrack>()


    /**
     * onSessionScreenReady()실행시 초기화됨.
     * 연결 객체를 by lazy 초기화로 생성함.
     * peerConnectionFactory.makePeerConnection()를 이용해 객체 생성.
     */
//    private val peerConnection: StreamPeerConnection by lazy {
//        peerConnectionFactory.makePeerConnection(
//        )
//    }





    init {
        //하나의 코루틴을 생성하여 WebRtc 세션매니저가 시작될때 생성된 SignalingClient 객체의 상태를 구독시킴.
        sessionManagerScope.launch {
            //signalingClient내의 SignalingCommand 값에 따라,
            // 해당 시그널 상황에서 클라이언트가 해야할 작업들인 handle**() 메소드가 실행된다.
            //handleAnswer(), handleIce()내에서 peerConnection 객체를 사용함.
            signalingClient.signalingCommandFlow.collect { pair ->
                //상태값의 키페어를 가져와 사용. second에는 보통 SPD 문자열이 담김.
                when (pair.first) {
                    //signalingCommandFlow의 state 값이 변할때마다 실행됨.
                    // OFFER가 두개면 두번 실행됨. 예)peer B와 C가 이곳 A에게 OFFER주는 상황 등.
                    // 이때, 이곳 A는 B와 C의 peerId를 확인하여 해당 아이디로 이미 생성된 peerConnection 객체가
                    // 있는지 map에서 먼저 확인해야함.
                    SignalingCommand.OFFER -> handleOffer(pair.second["peerId"].asString, pair.second["sdp"].asString)
                    SignalingCommand.ANSWER -> handleAnswer(pair.second["peerId"].asString, pair.second["sdp"].asString)
                    SignalingCommand.ICE -> handleIce(pair.second["peerIdOf"].asString, pair.second["sdp"].asString)
                    SignalingCommand.CLOSE -> handleClose(pair.second["peerId"]?.asString?:"none")
                    else -> Unit
                }
            }

//            signalingClient.방접속시도시접속인원목록.collect { userIds ->
//            }
        }
    }




    //-----------------------------------------------상속받은 WebRtcSessionManager의 impl(구현부) start
    /**
     * 기기의 화면이 준비됐을때 peerConnection객체를 by lazy에 의해 초기화함.
     * 처음은 VideoCallScreen() 컴포넌트 실행시(WebRTCSessionState.Ready상태)
     */
    override fun onSessionScreenReady() {

        setupAudio()
        // todo  각각의 피어에 대해 sendOffer를 호출해야함. 다만, 이미 연결되어있는 peerConnection의 경우 호출하면 안될듯.
        //  거기에 대한 확인절차에 대한 코드가 있어야할 것 같다.
        sessionManagerScope.launch {
            Log.e(tagName, "onSessionScreenReady()실행. signalingClient.sessionStateFlow.value: ${signalingClient.sessionStateFlow.value}")

            // sending local video track to show local video from start
            //카메라와 연결되어 만들어진 비디오 트랙 객체를 상태값으로 할당.
            //emit을 쓰는 이유는 일단 코루틴 스코프내에서의 실행인 점과, 로컬비디오트랙을 할당하면 로컬비디오트랙에 새로운 값으로 채워지는(변경)
            //타이밍까지 코루틴을 잠시 멈춤. 즉, 로컬비디오트랙의 초기화 작업과 카메라로부터 시작되는 실행을 마칠때까지, 흐름제어를 해주는 역할임.
            // 이것을 네트워크 통신에 비유하면, 각각의 http통신이 마치고 response가 올때까지 기다리는 것과 비슷함. await 해주는 역할.

            // todo 현재 접속할 방의 정보(접속한 peer들의 id)를 받아와 반복문으로 각각의 peer에 OFFER 요청을 해야함.
            //  주의: 최신 정보를 담고 있지는 않음. '참가' 버튼을 누르기까지 대기시간이 존재해서, 실제 방참가시 시간차가 있음.
            signalingClient.방참가시접속인원목록.value.forEach { userInfo ->
                val peerId = userInfo.asJsonObject["userId"].asString
                if(peerId != MyApp.userInfo.user_email){ // 방접속원의 아이디가 본인이라면 오퍼 안해야함.
                    sendOffer(peerId)
                }
            }

            _localVideoTrackFlow.emit(localVideoTrack)
        }
    }

    /**
     * 다음 카메라 id로 전환
     * */
    override fun flipCamera() {
        (videoCapturer as? Camera2Capturer)?.switchCamera(null)
    }

    /**
     * 마이크 on/off - 오디오 트랙 자체를 제거하는건 아니고 안드로이드 기기의 마이크를 조절하는 것.
     * */
    override fun enableMicrophone(enabled: Boolean) {
        audioManager?.isMicrophoneMute = !enabled
    }

    /**
     * 카메라 on/off
     * */
    override fun enableCamera(enabled: Boolean) {
        if (enabled) {
            videoCapturer.startCapture(resolution.width, resolution.height, 20)
        } else {
            videoCapturer.stopCapture()
        }
    }



    /**
     * 원격 & 로컬 비디오, 오디오 트랙의 캐시를 videoTrack.dispose()로 비운다.
     * 로컬의 비디오 & 오디오 트랙을 dispose()함.
     * audioHandler, videoCapturer 를 stop() 후 disopose()
     * 마지막으로, signalingClient.dispose()를 이용해 웹소켓을 닫는다.
     */
    override fun disconnect() {
        // dispose audio & video tracks.
//        remoteVideoTrackFlow.replayCache.forEach { videoTrack ->
//            videoTrack.dispose()
//        }
        remoteVideoTracks.replayCache.forEach { map ->
            map.forEach {
                it.dispose()
            }
        }
        Log.e(tagName, "Session disconnect() 1")
        localVideoTrackFlow.replayCache.forEach { videoTrack ->
            videoTrack.dispose()
        }
        Log.e(tagName, "Session disconnect() 2")
        localAudioTrack.dispose()
        Log.e(tagName, "Session disconnect() 3")
        localVideoTrack.dispose()
        Log.e(tagName, "Session disconnect() 4")

        // dispose audio handler and video capturer.
        audioHandler.stop()
        Log.e(tagName, "Session disconnect() 5")
        videoCapturer.stopCapture()
        Log.e(tagName, "Session disconnect() 6")
        videoCapturer.dispose()
        Log.e(tagName, "Session disconnect() 7")

        //웹소켓 연결을 해제하기 전에 먼저 peer 연결부터 끊어야된다. 순서가 바뀌면 에러로 앱이 강종된다.
        // 다자간에서는 연결을 끊은 대상 peer의 Id를 peerConnections 맵에서 찾아서 끊어야함.
//        peerConnection.connection.dispose()
//        peerConnectionFactory.factory.stopAecDump()

        // dispose signaling clients and socket.
        Log.e(tagName, "Session disconnect() 8")
        signalingClient.dispose()

//        Log.e(tagName, "Session disconnect() 9")
    }



    private var isDisconnected = false
    /**
     * WebRtc 세션이 disconnect 되었는지 확인하는 함수.
     * RtcFm의 destroy 생명주기때 실행되어 확인해야함. 이미 종료되었다면 nullporint Exception이 뜰것이기 때문.
     */
    override fun isDisconnected(): Boolean { //단순 확인용
        return isDisconnected
    }
    override fun isDisconnected(execute:Boolean): Boolean { //VideoCallScreen.kt에서 통화 끊을때 변경용.
        isDisconnected = execute
        return isDisconnected
    }
    //-----------------------------------------------상속받은 WebRtcSessionManager의 impl(구현부) End







    /**
     * 어떤 피어의 연결이 종료 또는 끊키면 피어연결을 종료해줌.
     */
    private fun handleClose(peerId: String) {
        if (peerConnections.containsKey(peerId)) {
            Log.e(tagName, "handleClose() peerId 제거: $peerId")
            // todo  화면에 관련된 videoFlow들도 리스트에서 제거해줘야함

            sessionManagerScope.launch {
                //현재 플로우에 있는 리스트를 가져옴.
                val currentVideoTracks = _remoteVideoTracks.first()
                // 조건에 맞지 않는 VideoTrack만 포함하는 새로운 리스트를 생성.
                val updatedVideoTracks = currentVideoTracks.filter { videoTrack ->
                    val currentPeerId = videoTrack.peerInfo["peerId"]
                    currentPeerId != peerId
                }
                _remoteVideoTracks.emit(updatedVideoTracks)

            }
            peerConnections[peerId]!!.connection.dispose() //native webrtc ndk lib에서 c++객체에 할당된 메모리자원을 해제해줌.
            peerConnections.remove(peerId)
        }
    }




    /**
     * Offer 받았을때, 상대방 peerId를 이용하여 peerConnection 객체를 생성하고,
     * 내 로컬 카메라를 이용한 영상 트랙을 그 peerConnection 객체에 addTrack해줌.
     */
    private suspend fun createPeer(peerId: String) {
        Log.e(tagName, "createPeer() peerId로 peerConnection 생성: $peerId")
        val newPeerConnection = createPeerConnection(peerId)
        peerConnections[peerId] = newPeerConnection
//        newPeerConnection.connection.restartIce()
        //내 로컬 카메라를 이용해 가져온 영상트랙을 이 peerConnection 객체에 추가함.
        // - 현재 연결된 상대 peer에게 내 영상을 전달해야하기 때문.
        newPeerConnection.connection.addTrack(localVideoTrack)
        newPeerConnection.connection.addTrack(localAudioTrack)

    }

    /**
     * 해당 peer가 종료되거나 끊켰을때, 해당 피어의 peerConnection객체를 정상적으로 제거해줘야함.
     */
    private fun removePeer(peerId: String) {
        peerConnections[peerId]?.connection?.dispose() //native webrtc ndk lib에서 c++객체에 할당된 메모리자원을 해제해줌.
        peerConnections.remove(peerId)
    }

    /**
     * onSessionScreenReady()안에서 사용
     * 처음으로 시그널링 서버로 Offer 명령을 보냄.
     *
     * Offer-Answer handshake 과정 순서: onSessionScreenReady() -> sendOffer() -> handleOffer()
     * -> sendAnswer() -> handleAnswer()
     */
    private suspend fun sendOffer(peerIdOfTarget: String) {
        // todo  새로운 절차: 자신의 피어연결 객체를 만들고, 추가해놓음. 그리고, offer sdp를 만들어서 자신의 peerConnection
        //  객체에 setLocalDescription 으로 등록하고, 서버로 그 sdp와 자신의 peerId를 담아 Offer명령을 보냄.
        //  그렇다면, 어떤 peerId에 대해 요청해야하는가의 문제가 생김. 오퍼전 웹소켓으로 방접속시 방에 접속한 peer들의 Id를
        //  가져와 그 peerId들에 전부 sendOffer하는게 맞을까? 그들 각각에 대해 sendOffer해주는게 맞는 것 같다.
        //  sendOffer를 for문으로 반복해서 rooms안에 있는 peerId 전부에게 offer보내야함.
        createPeer(peerIdOfTarget)
        val peerConnection = peerConnections[peerIdOfTarget] ?: return
        //suspendCoroutine 의 Result<T>객체가 success인지 failure인지에 따라 그 결과값이 가지고 있는 T객체값을 반환함.
        //offer ==  SessionDescription == SDP 객체를 생성해서
        val offer = peerConnection.createOffer().getOrThrow()
        //setLocalDescription를 완료할때까지 suspend나 await 등으로 기다리지말고 해야 안끊킨다는 얘기가 있어서 먼저 보내는걸로 수정해봄.
        signalingClient.sendCommand(SignalingCommand.OFFER, MyApp.userInfo.user_email, offer.description, peerIdOfTarget)
        // 자신의 SDP를 생성하여 로컬 SDP로 등록 후
        val result = peerConnection.setLocalDescription(offer)
        // Result 객체가 성공적으로 만들어지면, 타겟피어로 OFFER명령을 시그널링서버를 통해 전송.
        result.onSuccess {
            // todo 여기서 sdp만 달랑 보내는게 아니라 자신의 peerId와 같이 보내야함.
            Log.e(tagName, "sendOffer() 다음에게 OFFER보냄: $peerIdOfTarget")
//            signalingClient.sendCommand(SignalingCommand.OFFER, MyApp.userInfo.user_email, offer.description, peerIdOfTarget)
        }
//        logger.d { "[SDP를 서버로 전송] sendOffer(): ${offer.stringify()}" }
    }


    /**
     * init{} 안에서 사용. Answer를 보낼 peer가 실행할 메소드임.
     * 서버로부터(타 peer) 받은 Offer정보를 원격 sdp로써 설정.
     *
     * 타 peer로부터 Offer를 받으면 그 정보(jsonObject로 받아야할듯)를 이용해 해당 peer에 대한 peerConnection
     * 객체를 만들고, 그 객체에 원격 SDP를 설정함. 그리고, 그에 따라 나의 local SDP도 여기에 설정을 해줘야하니
     * sendAnswer()에서 그 작업을 수행함.
     */
//    private fun handleOffer(sdp: String) {
////        logger.d { "[$tagName] handleOffer() SDP 문자열: $sdp" }
//        offer = sdp
//    }
    private suspend fun handleOffer(peerIdOfOffered: String, sdp: String) {
        createPeer(peerIdOfOffered)
        val peerConnection = peerConnections[peerIdOfOffered] ?: return
//        val peerConnection = peerConnections[peerIdOfOffered]?: createPeerConnection(peerIdOfOffered)
        //타 peer로부터온 offer 메시지를 원격sdp로써 설정,
        peerConnection.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdp))
        sendAnswer(peerIdOfOffered)
    }


    /**
     * onSessionScreenReady()안에서 사용.
     * 다른 peer의 Offer에 대한 Answer로써 Local SDP를 보낼때 사용.
     */
    private suspend fun sendAnswer(peerIdOfOffered: String) {

//        peerConnection.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offer))
        val peerConnection = peerConnections[peerIdOfOffered] ?: return
//        peerConnection.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdp))

        //타 peer에 보내기 위한 answer로써의 sdp를 생성함.
        val answer = peerConnection.createAnswer().getOrThrow()
        //setLocalDescription를 완료할때까지 suspend나 await 등으로 기다리지말고 해야 안끊킨다는 얘기가 있어서 먼저 보내는걸로 수정해봄.
        signalingClient.sendCommand(SignalingCommand.ANSWER, MyApp.userInfo.user_email, answer.description, peerIdOfOffered)
        //그리고, 생성된 나의 sdp를 로컬sdp로써 설정.
        val result = peerConnection.setLocalDescription(answer)
        result.onSuccess {
            //나의 로컬 sdp를 내 peerId와 함께 offer준 peerId에게 응답해줘야함.
            // 나는 이런 아이디이고, 오퍼한 아이디에게 나의 sdp를 전달하고 싶다는 메시지 보냄.
//            signalingClient.sendCommand(SignalingCommand.ANSWER, MyApp.userInfo.user_email, answer.description, peerIdOfOffered)
            Log.e(tagName, "sendAnswer() 다음에게 ANSWER보냄: $peerIdOfOffered")
        }
//        logger.d { "[SDP를 서버로 전송] sendAnswer(): ${answer.stringify()}" }
    }




    /**
     * init{} 안에서 사용. 처음 Offer한 peer가 받는(실행할) 메소드임.
     * 서버로부터(타 peer) 받은 Answer정보를 원격 sdp로써 설정.
     */
    private suspend fun handleAnswer(peerIdOfAnswered: String, sdp: String) {
//        logger.d { "[$tagName] handleAnswer() SDP 문자열: $sdp" }
        val peerConnection = peerConnections[peerIdOfAnswered]!! /*?: createPeerConnection(peerIdOfAnswered)*/
        peerConnection.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    /**
     * init{} 안에서 사용
     * peerConnection.addIceCandidate() 명령 실행.
     */
    private suspend fun handleIce(peerIdOf: String, iceMessage: String) {
        // todo  offer peerId와 answer peerId 둘다 send OFFER시에 보내야 될려나? 아니면 그냥 각각의 파트때
        //  각각의 peerId로 처리되게끔 놔둘까나? ICE메시지가 offer와 answer 될 양쪽의 peerId 모두에게 전달되어야할지 생각.
//        logger.d { "[$tagName] handleIce() iceMessage 문자열: $iceMessage" }
        val iceArray = iceMessage.split(ICE_SEPARATOR)
        // todo 여기 peerId는 상대 peer에서 peerConnection객체 생성시 ICE용으로 peerId를 제공했던 id인데,
        //   그것이 그대로 ICE sendCommand()메소드에 전달되어 시그널링서버를 거쳐 그 id로 소켓을 찾고, 여기로 전달되어온다.
        //  그런데, 정작 여기 peerConnection객체의 id는 IceCandidate를 보내온 peerId가 아닌 나의 peerId이기에
        //  밑의 peerConnections Map에서 상대 peerId로 생성된 객체가 없어서 return 되고, addIceCandidate는 실행되지 않는다.
        //  즉, 전달은 되고 있지만, add는 되지 않고있다. log에서도 addIceCandidate가 실행된 흔적이 없다!
        //  Ice sendComand를 보내는 부분에 쓰기위해 자신의 peerId(MyApp.userInfo.user_email)도 같이 넣어 pc객체를 생성해야될듯.
        val peerConnection = peerConnections[peerIdOf] ?: return
        peerConnection.addIceCandidate(
            IceCandidate(
                iceArray[0],
                iceArray[1].toInt(),
                iceArray[2]
            )
        )
    }





    /**
     * 카메라 캡처 객체를 만들어 반환함.
     * */
    private fun buildCameraCapturer(): VideoCapturer {
        //일단 카메라 매니저는 호출되어 미리 변수에 초기화 되어 있어야함.
        Log.e(tagName, "buildCameraCapturer() 실행됨.")
        val manager = cameraManager ?: throw RuntimeException("CameraManager was not initialized!")
        Log.e(tagName, "buildCameraCapturer() manager: $manager")

        val ids = manager.cameraIdList
        var foundCamera = false
        var cameraId = ""

        for (id in ids) {
            val characteristics = manager.getCameraCharacteristics(id)
            //카메라 장치의 방향(앞뒤)를 가져옴. 앞뒤를 판별하는 기준은 장치(휴대폰등)의 화면이 나오는 방향이 앞(Front)임.
            val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            // 장치의 앞을 바라보는 카메라의 id인지 확인하고 맞으면 찾았다는 신호와 id를 변수에 저장.
            if (cameraLensFacing == CameraMetadata.LENS_FACING_BACK) {
                foundCamera = true
                cameraId = id
            }
        }

        //프론트 카메라를 찾지 못하면, 그냥 첫번째 카메라를 사용할 카메라로 넣어줌.
        if (!foundCamera && ids.isNotEmpty()) {
            cameraId = ids.first()
        }

        //안드로이드 카메라2로더 객체를 리턴해줌.
        val camera2Capturer = Camera2Capturer(context, cameraId, null)
        return camera2Capturer
    }


    /**
     * 오디오 제약 조건을 만들고 반환함. 에코감소, 자동컨트롤, 필터, 노이즈 감소, 타자치는 듯한 노이즈 감소등 적용한 제약조건 반환.
    * */
    private fun buildAudioConstraints(): MediaConstraints {
        val mediaConstraints = MediaConstraints()
        val items = listOf(
            MediaConstraints.KeyValuePair(
                "googEchoCancellation",
                true.toString()
            ),
            MediaConstraints.KeyValuePair(
                "googAutoGainControl",
                true.toString()
            ),
            MediaConstraints.KeyValuePair(
                "googHighpassFilter",
                true.toString()
            ),
            MediaConstraints.KeyValuePair(
                "googNoiseSuppression",
                true.toString()
            ),
            MediaConstraints.KeyValuePair(
                "googTypingNoiseDetection",
                true.toString()
            )
        )

        return mediaConstraints.apply {
            with(optional) {
                add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
                addAll(items)
            }
        }
    }


    /**
     * WebRtc 기능 사용중 사용할 오디오 장치를 찾아서 설정함.
     */
    private fun setupAudio() {
        logger.d { "[setupAudio] no args" }
        audioHandler.start()
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: return
            val deviceType = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

            //audioManager를 통해 찾은 사용 가능한 오디오장치들 중 SPEAKER 타입을 찾아서 반환해줌.
            val device = devices.firstOrNull { it.type == deviceType } ?: return

            //그리고, 사용할 커뮤니케이션 장치로써 그 장치를 설정해줌.
            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
            logger.d { "[setupAudio] isCommunicationDeviceSet: $isCommunicationDeviceSet" }
        }
    }













    /**
     * 화면공유 관련 변수 및 콜백. mediaProjectionManager 객체를 시스템으로부터 받아온다.
     */
    val mediaProjectionManager = ContextCompat.getSystemService(context, MediaProjectionManager::class.java)
    var mediaProjection : MediaProjection? =  null
    //    val screenShareForResult = (context as MainActivity)
//    var forScreenSharing: MainActivity.ResultMediaProjectionForRTCscreenSharing? = null
    var screenShareCapturer : VideoCapturer? = null



    fun 화면공유실행(){
        //MainActivity에 등록된 콜백으로 인텐트를 발송하여 결과를 받은뒤,
//        (context as MainActivity).register.launch(mediaProjectionManager!!.createScreenCaptureIntent())
        rtcFm.register.launch(mediaProjectionManager!!.createScreenCaptureIntent())

        //화면공유를 위해 포그라운드 서비스 실행 - 보안상 이유로 MediaProjection를 사용할려면 필요.
        val intentForScreenShare = Intent(context, MediaProjectionService::class.java)
        context.startForegroundService(intentForScreenShare)
    }

    fun 화면공유중지() {
        val intentForScreenShare = Intent(context, MediaProjectionService::class.java)
        screenShareCapturer?.stopCapture()
        resetVideoTrack(null, true)
        context.stopService(intentForScreenShare)
    }

    /**
     * 사용안함. 바탕화면만 공유가능한 상태. 제대로 구현할려면, 웹소켓을 RTC_FM에 두지않고, 앱 or FM실행시 서비스에서
     * 실행하도록 해야할듯. 그래야, RTC_FM을 나가서도 RTC가 작동하게 될듯.
     */
    fun 화면공유초기화(data: Intent){
//        screenShareForResult.launch(mediaProjectionManager!!.createScreenCaptureIntent())
        //MainActivity에 등록된 콜백으로 인텐트를 발송하여 결과를 받은뒤,
//        (context as MainActivity).register.launch(mediaProjectionManager!!.createScreenCaptureIntent())
        //결과를 반환하는 interface 객체를 생성하여 등록하고, 그것에 접근하여 null이아니면 그대로 실행하고,
//        if((context as MainActivity).forScreenSharing != null){
//            forScreenSharing = (context as MainActivity).forScreenSharing
//        }else{
//            // null이면 결과반환까지 딜레이를 약간 줘서 기다린 후, 다음 작업 진행.
//        }
        sessionManagerScope.launch {
//                forScreenSharing = (context as MainActivity).forScreenSharing
            Log.e(tagName, "화면공유초기화() ")
//                val data = forScreenSharing!!.intentDataCalled()
            assert (mediaProjectionManager != null)
            mediaProjection = mediaProjectionManager?.getMediaProjection(Activity.RESULT_OK, data)
            assert (mediaProjection != null)
            createScreenSharingPeerConnection(mediaProjection!!)
        }
    }

    /**
     * 화면공유를 위해 비디오 트랙을 만들고, 그것을 peerConnetion 객체의 sender에 등록하거나, 새로운 view에서 사용하도록
     * sessionManagerimple에서 목록화하기.
     */
    private fun createScreenSharingPeerConnection(mediaProjection: MediaProjection) {

        screenShareCapturer = createScreenCapturer(mediaProjection)
        val videoSource = peerConnectionFactory.factory.createVideoSource(screenShareCapturer!!.isScreencast).apply {
            screenShareCapturer!!.initialize(
                SurfaceTextureHelper.create(
                    "ScreenShareSurfaceTextureHelperThread",
                    peerConnectionFactory.eglBaseContext
                ),
                context,
                capturerObserver
            )

            // api31이상일때는 jetpack 호환성없는 lib로, api30이하~14까지일때는 호환성 lib로 기기 화면의 크기를 받아와야함.
            // 이때, 앱의 경계면을 받는게 아니라, 전체화면을 받아와야하기에 이런 메소드를 사용함.
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                val windowContext = context.createWindowContext(
                    context.display!!,
                    WindowManager.LayoutParams.TYPE_APPLICATION, null)
                val projectionMetrics = windowContext.getSystemService(WindowManager::class.java).maximumWindowMetrics
                screenShareCapturer!!.startCapture(projectionMetrics.bounds.width(), projectionMetrics.bounds.height(), 15)
            } else {
                val projectionMetrics = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(context as MainActivity)
                screenShareCapturer!!.startCapture(projectionMetrics.bounds.width(), projectionMetrics.bounds.height(), 15)
            }
        }

        val screenSharingVideoTrack = peerConnectionFactory.factory.createVideoTrack("lmsSV1", videoSource)
        screenSharingVideoTrack.setEnabled(true)

        // Add the screen sharing track to the local media stream
        //그냥 현재 connection sender에 addTrack해주면안되나? 상관없을 것 같은데?
//        val localMediaStream = factory.createLocalMediaStream("lms")
//        localMediaStream.addTrack(screenSharingVideoTrack)
        //별개의 역할인듯. 미디어스트림은 원격피어에게 이 트랙이 어떤 미디어스트림에 속해있는지 확인한후 그 미디어스트림
        //을 사용할지 정할때 사용되는 역할.
//        localMediaStream.id //미디어스트림의 고유id. 생성될때 만들어짐.


        // Replace the local video track with the screen sharing track in the PeerConnection
        // senders를 불러오면 ndk lib로부터 갱신된 새로운 senders 리스트를 가져옴.
//        val sender = connection.senders.find { it.track()?.id() == "lmsSV1" }
        // todo  각 peerConnection에 있는 sender의 video track을 찾아서 공유화면으로 교체해줌. 일단 테스트용.
        resetVideoTrack(screenSharingVideoTrack)

        // If you want to switch back to the camera video track, you can call
        // 이후 만들어둔 localVideoTrack을 밑의 sender를 찾아서 넣어주거나, 그냥 view를 하나더 추가하는 방식으로 조정.
        // sender.setTrack(cameraVideoTrack, true) later
    }

    /**
     * VideoTrack을 재설정함. 각 peerConnection객체내 sender List에서 videoTrack을 찾아서 받아온 Track으로 교체.
     */
    fun resetVideoTrack(videoTrackToSet: VideoTrack? = null, isLocalVideoSet:Boolean = false) {
        if (isLocalVideoSet) {
            peerConnections.forEach {
                it.value.setTrackToConnectionSender(localVideoTrack)
            }

        } else {
            peerConnections.forEach {
                it.value.setTrackToConnectionSender(videoTrackToSet)
            }
        }
    }


    /**
     * 화면 공유에 필요한 MediaProjection 객체를 생성하고, 그 객체가 중단되었을때 동작할 콜백을 등록.
     */
    private fun createScreenCapturer(mediaProjection: MediaProjection): VideoCapturer {
        // ScreenCapturerAndroid객체를 초기화 할려면 VideoSource객체가 필요함.
        // 그것으로 ScreenCapturerAndroid.initialize()실행해야함.
        val screenCapturerAndroid = ScreenCapturerAndroid(
            context,
            mediaProjection,
            object: MediaProjection.Callback(){
                override fun onStop() {
                    super.onStop()
                    Log.e(tagName, "MediaProjection.Callback() onStop(): 권한없음?")
                }
            }
        )
        return screenCapturerAndroid
    }
    
    
    
    
    
    
    
    
}
