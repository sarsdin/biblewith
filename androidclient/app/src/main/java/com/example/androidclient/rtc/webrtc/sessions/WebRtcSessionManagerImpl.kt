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

package io.getstream.webrtc.sample.compose.webrtc.sessions

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
import io.getstream.log.taggedLogger
import com.example.androidclient.rtc.webrtc.SignalingClient
import com.example.androidclient.rtc.webrtc.SignalingCommand
import io.getstream.webrtc.sample.compose.webrtc.audio.AudioHandler
import io.getstream.webrtc.sample.compose.webrtc.audio.AudioSwitchHandler
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnection
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerType
import io.getstream.webrtc.sample.compose.webrtc.utils.stringify
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
    private val cameraManager by lazy { context.getSystemService<CameraManager>() }//카메라 메니저 호출해서 가져옴.
    private val videoCapturer: VideoCapturer by lazy { buildCameraCapturer() }
    private val cameraEnumerator: Camera2Enumerator by lazy { //
        Camera2Enumerator(context)
    }

    //카메라가 지원하는 해상도를 담고있는 객체를 받아옴.
    private val resolution: CameraEnumerationAndroid.CaptureFormat
        get() {
            val frontCamera = cameraEnumerator.deviceNames.first { cameraName ->
                cameraEnumerator.isFrontFacing(cameraName)
            }
            val supportedFormats = cameraEnumerator.getSupportedFormats(frontCamera) ?: emptyList()
            return supportedFormats.firstOrNull {
                (it.width == 1280 || /*it.width == 1920 ||*/ it.width == 720 || it.width == 480 || it.width == 360)
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
            videoCapturer.startCapture(resolution.width, resolution.height, 30)
        }
    }

    /**
     * 로컬 비디오 트랙을 팩토리로부터 생성함. 잘보면 videoSource객체를 peer팩토리에 넣어서 VideoTrack를 생성함
     * peerConnectionFactory가 videoSource, VideoTrack 객체 둘다 생성하는 것을 알 수 있다.
     * */
    private val localVideoTrack: VideoTrack by lazy {
        peerConnectionFactory.makeVideoTrack(
            source = videoSource,
            trackId = "Video${UUID.randomUUID()}"
        )
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
            onIceCandidateRequest = { iceCandidate, _ ->
                signalingClient.sendCommand(
                    SignalingCommand.ICE,
                    "${iceCandidate.sdpMid}$ICE_SEPARATOR${iceCandidate.sdpMLineIndex}$ICE_SEPARATOR${iceCandidate.sdp}"
                )
            },
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
            //signalingClient내의 SignalingCommand 값에 따라 handle**() 메소드가 실행된다.
            //handleAnswer(), handleIce()내에서 peerConnection 객체를 사용함.
            signalingClient.signalingCommandFlow.collect { commandToValue ->
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
     */
    override fun onSessionScreenReady() {
        setupAudio()
        peerConnection.connection.addTrack(localVideoTrack)
        peerConnection.connection.addTrack(localAudioTrack)
        sessionManagerScope.launch {
            // sending local video track to show local video from start
            _localVideoTrackFlow.emit(localVideoTrack)

            if (offer != null) {
                sendAnswer()
            } else {
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
            videoCapturer.startCapture(resolution.width, resolution.height, 30)
        } else {
            videoCapturer.stopCapture()
        }
    }

    override fun disconnect() {
        // dispose audio & video tracks.
        remoteVideoTrackFlow.replayCache.forEach { videoTrack ->
            videoTrack.dispose()
        }
        localVideoTrackFlow.replayCache.forEach { videoTrack ->
            videoTrack.dispose()
        }
        localAudioTrack.dispose()
        localVideoTrack.dispose()

        // dispose audio handler and video capturer.
        audioHandler.stop()
        videoCapturer.stopCapture()
        videoCapturer.dispose()

        // dispose signaling clients and socket.
        signalingClient.dispose()
    }
    //-----------------------------------------------상속받은 WebRtcSessionManager의 impl(구현부) End






    /**
     * onSessionScreenReady()안에서 사용
     */
    private suspend fun sendOffer() {
        //suspendCoroutine 의 Result<T>객체가 success인지 failure인지에 따라 그 결과값이 가지고 있는 T객체값을 반환함.
        val offer = peerConnection.createOffer().getOrThrow()
        val result = peerConnection.setLocalDescription(offer)
        result.onSuccess {
            signalingClient.sendCommand(SignalingCommand.OFFER, offer.description)
        }
        logger.d { "[SDP] send offer: ${offer.stringify()}" }
    }


    /**
     * onSessionScreenReady()안에서 사용
     */
    private suspend fun sendAnswer() {
        peerConnection.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offer))

        val answer = peerConnection.createAnswer().getOrThrow()
        val result = peerConnection.setLocalDescription(answer)
        result.onSuccess {
            signalingClient.sendCommand(SignalingCommand.ANSWER, answer.description)
        }
        logger.d { "[SDP] send answer: ${answer.stringify()}" }
    }

    /**
     * init{} 안에서 사용
     */
    private fun handleOffer(sdp: String) {
        logger.d { "[SDP] handle offer: $sdp" }
        offer = sdp
    }

    /**
     * init{} 안에서 사용
     */
    private suspend fun handleAnswer(sdp: String) {
        logger.d { "[SDP] handle answer: $sdp" }
        peerConnection.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    /**
     * init{} 안에서 사용
     */
    private suspend fun handleIce(iceMessage: String) {
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
        val manager = cameraManager ?: throw RuntimeException("CameraManager was not initialized!")

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
        logger.d { "[setupAudio] #sfu; no args" }
        audioHandler.start()
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: return
            val deviceType = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

            val device = devices.firstOrNull { it.type == deviceType } ?: return

            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
            logger.d { "[setupAudio] #sfu; isCommunicationDeviceSet: $isCommunicationDeviceSet" }
        }
    }
}
