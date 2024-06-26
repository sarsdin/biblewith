package jm.preversion.biblewith.rtc.webrtc.peer
import android.util.Log

import android.content.Context
import android.os.Build
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.rtc.webrtc.sessions.ChatData
import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SimulcastVideoEncoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class StreamPeerConnectionFactory constructor(
    val context: Context
) {
    private val webRtcLogger by taggedLogger("[StreamPeerConnectionFactory] Call:WebRTC")
    private val audioLogger by taggedLogger("[StreamPeerConnectionFactory] Call:AudioTrackCallback")

    val tagName = "[${this.javaClass.simpleName}]"

    //EGL(openGL) - 각 네이티브 플랫폼(==Android등)과 윈도우 시스템(==GUI) 사이의 Interface.
    val eglBaseContext: EglBase.Context by lazy {
        EglBase.create().eglBaseContext
    }


    // rtc 컨피그 객체를 생성.
    // rtcConfig contains STUN and TURN servers list
    val rtcConfig = PeerConnection.RTCConfiguration(
        arrayListOf(
            // adding google's standard server
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
//            PeerConnection.IceServer.builder("stun:ntk-turn-2.xirsys.com").createIceServer(),

//            PeerConnection.IceServer.builder("turn:54.245.2.121")
//                .setUsername("jin")
//                .setPassword("0115")
//                .createIceServer()

            PeerConnection.IceServer.builder(arrayListOf(
                "turn:59.24.179.98:3478?transport=tcp",
                "turn:59.24.179.98:3478?transport=udp",
                "turn:59.24.179.98:5349?transport=tcp",
                "turn:59.24.179.98:3478?transport=udp"
            ))
                .setUsername("jm")
                .setPassword("seol")
                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                .createIceServer(),


//            PeerConnection.IceServer.builder(arrayListOf(
//                "turn:ntk-turn-2.xirsys.com:80?transport=udp",
//                "turn:ntk-turn-2.xirsys.com:3478?transport=udp",
//                "turn:ntk-turn-2.xirsys.com:80?transport=tcp",
//                "turn:ntk-turn-2.xirsys.com:3478?transport=tcp",
//                "turns:ntk-turn-2.xirsys.com:443?transport=tcp",
//                "turns:ntk-turn-2.xirsys.com:5349?transport=tcp"
//            ))
//                .setUsername("-0ARbD_AIUoirYur9I5tF9kTWP5Vdtv6P6wy4zZto8dGVSI9XOmQZCrfr6stuwmNAAAAAGQL4I1qZXlzMTQ=")
//                .setPassword("632a2c28-bfb0-11ed-99c8-0242ac120004")
//                .createIceServer(),

//            PeerConnection.IceServer.builder("turn:numb.viagenie.ca")
//                .setUsername("webrtc@live.com")
//                .setPassword("muazkh")
//                .createIceServer(),

//            PeerConnection.IceServer.builder("stun:relay.metered.ca:80").createIceServer()
//            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
//            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
//            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
//            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
//            PeerConnection.IceServer.builder("stun:stunserver.org").createIceServer(),
//            PeerConnection.IceServer.builder("stun:stun.softjoys.com").createIceServer(),
//            PeerConnection.IceServer.builder("stun:stun.voiparound.com").createIceServer()
        )
    ).apply {
        // it's very important to use new unified sdp semantics PLAN_B is deprecated
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

    }




    //인코더 & 디코더 초기화 변수들
    /**
     * Default video decoder factory used to unpack video from the remote tracks.
     */
    private val videoDecoderFactory by lazy {
        DefaultVideoDecoderFactory(
            eglBaseContext
        )
    }
    /**
     * Default encoder factory that supports Simulcast, used to send video tracks to the server.
     */
    private val videoEncoderFactory by lazy {
        val hardwareEncoder = HardwareVideoEncoderFactory(eglBaseContext, true, true)
       SimulcastVideoEncoderFactory(hardwareEncoder, SoftwareVideoEncoderFactory())
    }



    /**
     * Factory that builds all the connections based on the extensive configuration provided under
     * the hood.
     * 중요한 것. peerConnection Factory 빌더를 이용해서 객체를 생성. 그 후 초기화됨.
     */
    val factory: PeerConnectionFactory by lazy {
        //'피어 연결 생성에 쓰이는 팩토리'에 연결시 일어나는 메시지 로깅을 등록을 위한 Option객체 생성하여 초기화 등록.
        PeerConnectionFactory.initialize(
            //PeerConnectionFactory내의 InitializationOptions 스태틱객체를 만들기위해 Builder객체를 이용해 변수들을 수집한 후
            //최종적으로 생성된 InitializationOptions객체를 PeerConnectionFactory에서 초기화(로깅,추적등)용도로 사용함.
            // -참고- : new Builder()의 멤버변수중 private String nativeLibraryName = "jingle_peerconnection_so"; 이것이 ndk lib 로드명임.
            // 이후 initialize()로 초기화할때 NativeLibrary.initialize() 이 변수를 실행하여 최종적으로 System.loadLibrary(name);를 사용하게 함.
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setInjectableLogger({ message, severity, label ->
                    when (severity) {
                        Logging.Severity.LS_VERBOSE -> {
//                            webRtcLogger.v { "[onLogMessage] label: $label, message: $message" }
                        }
                        Logging.Severity.LS_INFO -> {
//                            webRtcLogger.i { "[onLogMessage] label: $label, message: $message" }
                        }
                        Logging.Severity.LS_WARNING -> {
//                            webRtcLogger.w { "[onLogMessage] label: $label, message: $message" }
                        }
                        Logging.Severity.LS_ERROR -> {
//                            webRtcLogger.e { "[onLogMessage] label: $label, message: $message" }
                        }
                        Logging.Severity.LS_NONE -> {
//                            webRtcLogger.d { "[onLogMessage] label: $label, message: $message" }
                        }
                        else -> {}
                    }
                }, Logging.Severity.LS_VERBOSE)
                .createInitializationOptions()
        )

        // PeerConnection 객체 생성시 활용할 비디오&오디오 인코더, 디코더를 등록
        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .setAudioDeviceModule(
                //오디오 장치 모듈 객체를 생성할 수 있는 유틸리티 클래스를 이용하여 각 인수들 빌드함.
                JavaAudioDeviceModule.builder(context)
                    //안드로이드 10이상이면 에코와 노이즈 감소 기능을 on 함.
                    .setUseHardwareAcousticEchoCanceler(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    .setUseHardwareNoiseSuppressor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    //오디오 녹음 에러시 콜백 등록
                    .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                        override fun onWebRtcAudioRecordInitError(p0: String?) {
                            audioLogger.w { "[onWebRtcAudioRecordInitError] $p0" }
                        }

                        override fun onWebRtcAudioRecordStartError(
                            p0: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                            p1: String?
                        ) {
                            audioLogger.w { "[onWebRtcAudioRecordInitError] $p1" }
                        }

                        override fun onWebRtcAudioRecordError(p0: String?) {
                            audioLogger.w { "[onWebRtcAudioRecordError] $p0" }
                        }
                    })
                    //오디오 트랙 에러시 콜백 등록
                    .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                        override fun onWebRtcAudioTrackInitError(p0: String?) {
                            audioLogger.w { "[onWebRtcAudioTrackInitError] $p0" }
                        }

                        override fun onWebRtcAudioTrackStartError(
                            p0: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                            p1: String?
                        ) {
                            audioLogger.w { "[onWebRtcAudioTrackStartError] $p0" }
                        }

                        override fun onWebRtcAudioTrackError(p0: String?) {
                            audioLogger.w { "[onWebRtcAudioTrackError] $p0" }
                        }
                    })
                    //오디오 녹음 상태를 나타내는 콜백 등록
                    .setAudioRecordStateCallback(object : JavaAudioDeviceModule.AudioRecordStateCallback {
                        override fun onWebRtcAudioRecordStart() {
                            audioLogger.d { "[onWebRtcAudioRecordStart] no args: WebRtc 오디오 레코딩 시작" }
                        }

                        override fun onWebRtcAudioRecordStop() {
                            audioLogger.d { "[onWebRtcAudioRecordStop] no args: WebRtc 오디오 레코딩 종료" }
                        }
                    })
                    //오디오 트랙 상태를 나타내는 콜백 등록
                    .setAudioTrackStateCallback(object : JavaAudioDeviceModule.AudioTrackStateCallback {
                        override fun onWebRtcAudioTrackStart() {
                            audioLogger.d { "[onWebRtcAudioTrackStart] no args: WebRtc 오디오 트랙 시작" }
                        }

                        override fun onWebRtcAudioTrackStop() {
                            audioLogger.d { "[onWebRtcAudioTrackStop] no args: WebRtc 오디오 트랙 종료" }
                        }
                    })
                    //위에서 빌드된 각 인수들을 활용하는 오디오 장치 모듈 인스턴스 객체를 생성함.
                    .createAudioDeviceModule().also {
                        //초기 생성시 마이크와 스피커를 On 함.
                        it.setMicrophoneMute(false)
                        it.setSpeakerMute(false)
                    }
            )
            // PeerConnectionFactory 객체 생성을 위한 모든 인수들을 빌들했으면 마지막으로 그것을 활용하여 객체 생성.
            .createPeerConnectionFactory()
    }




    /**
     * Builds a [StreamPeerConnection] that wraps the WebRTC [PeerConnection] and exposes several
     * helpful handlers.
     *
     * @param coroutineScope Scope used for asynchronous operations.
     * @param configuration The [PeerConnection.RTCConfiguration] used to set up the connection.
     * @param type The type of connection, either a subscriber of a publisher.
     * @param mediaConstraints Constraints used for audio and video tracks in the connection.
     * @param onStreamAdded Handler when a new [MediaStream] gets added.
     * @param onNegotiationNeeded Handler when there's a new negotiation.
     * @param onIceCandidateRequest Handler whenever we receive [IceCandidate]s.
     * @return [StreamPeerConnection] That's fully set up and can be observed and used to send and
     * receive tracks.
     *
     * 피어커넥션보다는 팩토리가 먼저 만들어짐. impl에서의 순서가 그렇다. 나중에 피어커넥션만들때 이 메소드 실행함.
     */
    fun makePeerConnection(
        peerId: String,
        coroutineScope: CoroutineScope,
        configuration: PeerConnection.RTCConfiguration,
        type: StreamPeerType,
        mediaConstraints: MediaConstraints,
        onStreamAdded: ((MediaStream) -> Unit)? = null,
        onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)? = null,
        onIceCandidateRequest: ((IceCandidate, StreamPeerType) -> Unit)? = null,
        onVideoTrack: ((RtpTransceiver?) -> Unit)? = null,
        /**
         * 1: 받은 문자열 메시지, 2: file을 받으면 ByteArray를 전달, 3: TEXT or FILE 받은 메시지의 종류.
         */
//        onDataMessage: ((String?, ChatData?, String) -> Unit)? = null
        onDataMessage: ((String?, ByteArray?, String) -> Unit)? = null
    ): StreamPeerConnection {

        //받아온 정보(변수 및 실행할 콜백구현부들)를 이용해서 StreamPeerConnection 객체를 만든다.
        val peerConnection = StreamPeerConnection(
            peerId = peerId,
            context = context,
            streamFactory = this,
            coroutineScope = coroutineScope,
            type = type,
            mediaConstraints = mediaConstraints,
            onStreamAdded = onStreamAdded,
            onNegotiationNeeded = onNegotiationNeeded,
            onIceCandidate = onIceCandidateRequest,
            onVideoTrack = onVideoTrack,
            onDataMessage = onDataMessage
        )

        // PeerConnection.Observer의 구현부인 위에서 만든 StreamPeerConnection객체를 이용해서
        // StreamPeerConnection객체 내부에 실제 PeerConnection 객체를 만들고,
        val connection = makePeerConnectionInternal(
            configuration = configuration,
            observer = peerConnection
        )
        //내부 변수(connection)에 할당한다.
        return peerConnection.apply { initialize(connection) }
    }

    /**
     * 서버와 연결하고, 비디오 & 오디오 트랙을 주고받을 수 있는 peer 연결 객체임.
     * 그것을 factory를 이용해서 만듦.
     * Builds a [PeerConnection] internally that connects to the server and is able to send and
     * receive tracks.
     *
     * @param configuration The [PeerConnection.RTCConfiguration] used to set up the connection.
     * @param observer Handler used to observe different states of the connection.
     * @return [PeerConnection] that's fully set up.
     */
    private fun makePeerConnectionInternal(
        configuration: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer?
    ): PeerConnection {
        return requireNotNull(
            factory.createPeerConnection(
                configuration,
                observer
            )
        )
    }

    /**
     * 비디오 소스는 비디오 트랙에 비해 좀더 비디오 원본 생성에 관여하는 클래스임. CameraCapurer 클래스 등과 연계
     * 하여 비디오 영상을 담는 역할을 함. 비디오 트랙은 비디오 소스를 랩핑한 클래스로 peerConnection 객체를 통해
     * 다른 peer로 전송되기 위해 사용하는 클래스임. 결국, 계층으로 보면, Media - VideoTrack - VideoSource
     * Builds a [VideoSource] from the [factory] that can be used for regular video share (camera)
     * or screen sharing.
     *
     * @param isScreencast If we're screen sharing using this source.
     * @return [VideoSource] that can be used to build tracks.
     */
    fun makeVideoSource(isScreencast: Boolean): VideoSource =
        factory.createVideoSource(isScreencast)

    /**
     * Builds a [VideoTrack] from the [factory] that can be used for regular video share (camera)
     * or screen sharing.
     *
     * @param source The [VideoSource] used for the track.
     * @param trackId The unique ID for this track.
     * @return [VideoTrack] That represents a video feed.
     */
    fun makeVideoTrack(
        source: VideoSource,
        trackId: String
    ): VideoTrack {
        Log.e(tagName, "makeVideoTrack()실행: source: $source, trackId: $trackId")
        return factory.createVideoTrack(trackId, source).apply {
            peerInfo["userId"] = MyApp.userInfo.user_email
            peerInfo["nick"] = MyApp.userInfo.user_nick
        }
    }

    /**
     * Builds an [AudioSource] from the [factory] that can be used for audio sharing.
     *
     * @param constraints The constraints used to change the way the audio behaves.
     * @return [AudioSource] that can be used to build tracks.
     */
    fun makeAudioSource(constraints: MediaConstraints = MediaConstraints()): AudioSource =
        factory.createAudioSource(constraints)

    /**
     * Builds an [AudioTrack] from the [factory] that can be used for regular video share (camera)
     * or screen sharing.
     *
     * @param source The [AudioSource] used for the track.
     * @param trackId The unique ID for this track.
     * @return [AudioTrack] That represents an audio feed.
     */
    fun makeAudioTrack(
        source: AudioSource,
        trackId: String
    ): AudioTrack = factory.createAudioTrack(trackId, source)
}
