/*
 * Copyright 2023 Stream.IO, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidclient.rtc.webrtc.sessions
import android.util.Log

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.getSystemService
import com.example.androidclient.rtc.webrtc.SignalingClient
import com.example.androidclient.rtc.webrtc.SignalingCommand
import com.example.androidclient.rtc.webrtc.audio.AudioHandler
import com.example.androidclient.rtc.webrtc.audio.AudioSwitchHandler
import com.example.androidclient.rtc.webrtc.peer.StreamPeerConnection
import com.example.androidclient.rtc.webrtc.peer.StreamPeerConnectionFactory
import com.example.androidclient.rtc.webrtc.peer.StreamPeerType
import com.example.androidclient.rtc.webrtc.utils.stringify
import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
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
    private val context: Context,
    override val signalingClient: SignalingClient,
    override val peerConnectionFactory: StreamPeerConnectionFactory
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
    private val _remoteVideoTrackFlow = MutableSharedFlow<VideoTrack>()
    override val remoteVideoTrackFlow: SharedFlow<VideoTrack> = _remoteVideoTrackFlow

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
            return supportedFormats.firstOrNull {
                (it.width == 1280 || /*it.width == 1920 ||*/ it.width == 720 || it.width == 540 || it.width == 480 || it.width == 360)
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
//            videoCapturer.startCapture(resolution.width, resolution.height, 20)
            videoCapturer.startCapture(325, 490, 10)
            Log.e(tagName, "makeVideoSource() 실행. videoSource 초기화 완료.")
        }
    }

    /**
     * onSessionScreenReady()실행시 초기화. 즉, Ui sessionState상태값이 Ready일경우 발생.
     * 로컬 비디오 트랙을 팩토리로부터 생성함. 잘보면 videoSource객체를 peer팩토리에 넣어서 VideoTrack를 생성함
     * peerConnectionFactory가 videoSource, VideoTrack 객체 둘다 생성하는 것을 알 수 있다.
     * */
    val localVideoTrack: VideoTrack by lazy {
        Log.e(tagName, "localVideoTrack 초기화 시작.")
        peerConnectionFactory.makeVideoTrack(
            source = videoSource,
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
     * onSessionScreenReady()실행시 초기화됨.
     * 연결 객체를 by lazy 초기화로 생성함.
     * peerConnectionFactory.makePeerConnection()를 이용해 객체 생성.
     */
    private val peerConnection: StreamPeerConnection by lazy {

        peerConnectionFactory.makePeerConnection(
            coroutineScope = sessionManagerScope,
            configuration = peerConnectionFactory.rtcConfig,
            type = StreamPeerType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
            //Observer.onIceCandidate() 구현부에서 실행할 콜백임.
            // onIceCandidate() <<< 이 메소드는 Offer 시, Answer시 둘다 발동된다. 그래서, offer명령을 서버
            // 에 내리면 ice 명령이 실행되고, answer시에도 마찬가지로 ice 명령이 보내진다.
            // 새로운 iceCandidate가 들어오면 그것을 받아서 시그널링 서버로 ICE 시그널을 보냄.
            onIceCandidateRequest = { iceCandidate, _ ->
                signalingClient.sendCommand(
                    SignalingCommand.ICE,
                    "${iceCandidate.sdpMid}$ICE_SEPARATOR${iceCandidate.sdpMLineIndex}$ICE_SEPARATOR${iceCandidate.sdp}"
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
                    sessionManagerScope.launch {
                        _remoteVideoTrackFlow.emit(videoTrack)
                    }
                }
            }
        )
    }





    init {
        //하나의 코루틴을 생성하여 WebRtc 세션매니저가 시작될때 생성된 SignalingClient 객체의 상태를 구독시킴.
        sessionManagerScope.launch {
            //signalingClient내의 SignalingCommand 값에 따라,
            // 해당 시그널 상황에서 클라이언트가 해야할 작업들인 handle**() 메소드가 실행된다.
            //handleAnswer(), handleIce()내에서 peerConnection 객체를 사용함.
            signalingClient.signalingCommandFlow.collect { commandToValue ->
                //상태값의 키페어를 가져와 사용. second에는 보통 SPD 문자열이 담김.
                when (commandToValue.first) {
                    SignalingCommand.OFFER -> handleOffer(commandToValue.second)
                    SignalingCommand.ANSWER -> handleAnswer(commandToValue.second)
                    SignalingCommand.ICE -> handleIce(commandToValue.second)
                    else -> Unit
                }
            }
        }
    }





    //-----------------------------------------------상속받은 WebRtcSessionManager의 impl(구현부) start
    /**
     * 기기의 화면이 준비됐을때 peerConnection객체를 by lazy에 의해 초기화함.
     * 처음은 VideoCallScreen() 컴포넌트 실행시(WebRTCSessionState.Ready상태)
     */
    override fun onSessionScreenReady() {
        setupAudio()
        peerConnection.connection.restartIce()
        peerConnection.connection.addTrack(localVideoTrack)
        peerConnection.connection.addTrack(localAudioTrack)
        sessionManagerScope.launch {
            Log.e(tagName, "onSessionScreenReady()실행. signalingClient.sessionStateFlow.value: ${signalingClient.sessionStateFlow.value}")
            // sending local video track to show local video from start
            //카메라와 연결되어 만들어진 비디오 트랙 객체를 상태값으로 할당.
            _localVideoTrackFlow.emit(localVideoTrack)

            // 그후 offer 메시지(여기서는 다른 peer로부터 받은 SDP 문자열)
            // 유무에 따라 Offer or Answer 명령을 수행함.
            // 타 peer로부터 받은 SDP 문자열이 있으면 자신은 Answer로써 SDP를 보냄.
            if (offer != null) {
                sendAnswer()

            } else {
                //offer 메시지가 없으면 처음으로 시그널링 서버에 Offer를 주는 것임.
                //실행하면 SDP 문자열 생성과 함께 시그널링 서버로 Offer 명령을 보냄.
                sendOffer()
            }
        }
    }

    /**
     * 다음 카메라 id로 전환
     * */
    override fun flipCamera() {
        (videoCapturer as? Camera2Capturer)?.switchCamera(null)
    }

    /**
     * 마이크 on/off
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
        remoteVideoTrackFlow.replayCache.forEach { videoTrack ->
            videoTrack.dispose()
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
        peerConnection.connection.dispose()
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
     * onSessionScreenReady()안에서 사용
     * 처음으로 시그널링 서버로 Offer 명령을 보냄.
     */
    private suspend fun sendOffer() {
        //suspendCoroutine 의 Result<T>객체가 success인지 failure인지에 따라 그 결과값이 가지고 있는 T객체값을 반환함.
        //offer ==  SessionDescription == SDP 객체를 생성해서
        val offer = peerConnection.createOffer().getOrThrow()
        // 자신의 정보에 해당하는 설명을 SDP객체에 덧붙이고,
        val result = peerConnection.setLocalDescription(offer)
        // Result 객체가 성공적으로 만들어지면, OFFER 명령을 시그널링서버로 전송.
        result.onSuccess {
            signalingClient.sendCommand(SignalingCommand.OFFER, offer.description)
        }
//        logger.d { "[SDP를 서버로 전송] sendOffer(): ${offer.stringify()}" }
    }


    /**
     * onSessionScreenReady()안에서 사용.
     * 다른 peer에 answer로써 보낼때 사용.
     */
    private suspend fun sendAnswer() {
        //타 peer로부터온 offer 메시지를 원격sdp로써 설정하고,
        //(여기서 offer 변수는 signaling command로 Offer를 받고, handleOffer를 통해 할당되었던 원격의 offer sdp 이다.
        peerConnection.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offer))

        //타 peer에 보내기 위한 answer로써의 sdp를 생성함.
        val answer = peerConnection.createAnswer().getOrThrow()
        //그리고, 생성된 나의 sdp를 로컬sdp로써 설정.
        val result = peerConnection.setLocalDescription(answer)
        result.onSuccess {
            signalingClient.sendCommand(SignalingCommand.ANSWER, answer.description)
        }
//        logger.d { "[SDP를 서버로 전송] sendAnswer(): ${answer.stringify()}" }
    }








    /**
     * init{} 안에서 사용
     * 서버로부터(타 peer) 받은 Offer정보를 원격 sdp로써 설정.
     */
    private fun handleOffer(sdp: String) {
//        logger.d { "[$tagName] handleOffer() SDP 문자열: $sdp" }
        offer = sdp
    }

    /**
     * init{} 안에서 사용
     * 서버로부터(타 peer) 받은 Answer정보를 원격 sdp로써 설정.
     */
    private suspend fun handleAnswer(sdp: String) {
//        logger.d { "[$tagName] handleAnswer() SDP 문자열: $sdp" }
        peerConnection.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    /**
     * init{} 안에서 사용
     * peerConnection.addIceCandidate() 명령 실행.
     */
    private suspend fun handleIce(iceMessage: String) {
//        logger.d { "[$tagName] handleIce() iceMessage 문자열: $iceMessage" }
        val iceArray = iceMessage.split(ICE_SEPARATOR)
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
            if (cameraLensFacing == CameraMetadata.LENS_FACING_FRONT) {
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



    private fun setupAudio() {
        logger.d { "[setupAudio] no args" }
        audioHandler.start()
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: return
            val deviceType = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

            val device = devices.firstOrNull { it.type == deviceType } ?: return

            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
            logger.d { "[setupAudio] isCommunicationDeviceSet: $isCommunicationDeviceSet" }
        }
    }
}
