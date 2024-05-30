package jm.preversion.biblewith.rtc.webrtc.peer

import android.content.Context
import android.util.Log
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.rtc.webrtc.utils.addRtcIceCandidate
import jm.preversion.biblewith.rtc.webrtc.utils.createValue
import jm.preversion.biblewith.rtc.webrtc.utils.setValue
import jm.preversion.biblewith.rtc.webrtc.utils.stringify
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.getstream.log.taggedLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.*
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.*

/**
 * Wrapper around the WebRTC connection that contains tracks.
 *
 * @param coroutineScope The scope used to listen to stats events.
 * @param type The internal type of the PeerConnection. Check [StreamPeerType].
 * @param mediaConstraints Constraints used for the connections.
 * @param onStreamAdded Handler when a new [MediaStream] gets added.
 * @param onNegotiationNeeded Handler when there's a new negotiation.
 * @param onIceCandidate Handler whenever we receive [IceCandidate]s.
 */
class StreamPeerConnection(
    val peerId: String, //target의 peerId임(연결된 대상인 타peer)
    val context: Context,
    val streamFactory : StreamPeerConnectionFactory,
    private val coroutineScope: CoroutineScope,
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
    private val onVideoTrack: ((RtpTransceiver?) -> Unit)?,
//    private val onDataMessage: ((String?, ChatData?,String) -> Unit)?,
    private val onDataMessage: ((String?, ByteArray?,String) -> Unit)?,
) : PeerConnection.Observer {

    /**
     * StreamPeerType.PUBLISHER -> "publisher"
     * StreamPeerType.SUBSCRIBER -> "subscriber"
     * */
    private val typeTag = type.stringify()

    private val logger by taggedLogger("C:StreamPeerConnection")

    val tagName = "[${this.javaClass.simpleName}]"

    /**
     * The wrapped connection for all the WebRTC communication.
     * Peer connection 객체를 받아와서  initialize()로 초기화 함
     */
    lateinit var connection: PeerConnection
        private set

    /**
     * Used to manage the stats observation lifecycle.
     */
    private var statsJob: Job? = null

    /**
     * Used to pool together and store [IceCandidate]s before consuming them.
     */
    private val pendingIceMutex = Mutex()
    // peer connection을 위해 대기중인 IceCandidate를 저장할 List를 만듦
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    /**
     * Contains stats events for observation.
     */
    private val statsFlow: MutableStateFlow<RTCStatsReport?> = MutableStateFlow(null)


//    /**
//     * 화면공유 관련 변수 및 콜백. mediaProjectionManager 객체를 시스템으로부터 받아온다.
//     */
//    val mediaProjectionManager = getSystemService(context, MediaProjectionManager::class.java)
//    var mediaProjection : MediaProjection? =  null
////    val screenShareForResult = (context as MainActivity)
//    var forScreenSharing: MainActivity.ResultMediaProjectionForRTCscreenSharing? = null

//        .registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()){ result: ActivityResult ->
//        val resultCode = result.resultCode
//        val data = result.data
//
//        if(resultCode == Activity.RESULT_OK){
//            mediaProjection = mediaProjectionManager!!.getMediaProjection(resultCode, data!!)
//            createScreenSharingPeerConnection(mediaProjection!!)
//        }
//    }

    /**
     * 현재 peerConnection 객체를 이요해서 생성한 데이터채널
     */
    private lateinit var dataChannel: DataChannel
//    private lateinit var dataChannelForScreenShare: DataChannel


    private val receivedFileChunks = mutableMapOf<String, MutableList<ByteArray>>()

    init {
        logger.i { "<init> #$typeTag, mediaConstraints: $mediaConstraints" }
    }

    /**
     * Initialize a [StreamPeerConnection] using a WebRTC [PeerConnection].
     *
     * @param peerConnection The connection that holds audio and video tracks.
     */
    fun initialize(peerConnection: PeerConnection) {
        logger.d { "[Peer 연결 초기화] #$typeTag, peerConnection: $peerConnection" }
        this.connection = peerConnection
        logger.d { "[DataChannel 초기화] #$typeTag" }
        데이터채널초기화()
//        화면공유데이터채널초기화()
    }

    fun 데이터채널초기화(){
        // DataChannel 생성
        val dataChannelInit = DataChannel.Init()
//        dataChannelInit.ordered = true // 메시지 순서 보장 옵션
        dataChannel = connection.createDataChannel("chat", dataChannelInit)

        // dataChannel 을 여러개의 용도사용시 구분용(하나는 채팅과 파일전송, 하나는 화면공유 용도로써 생성할듯?)
        // dataChannel 을 어떤 맵에 넣고 맵의 이름을 label로 정하여 가져오기 가능.
//        dataChannel.label()
        // DataChannel 이벤트 핸들러 설정
        dataChannel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {
                // 버퍼 크기 변경 이벤트를 처리할 수 있습니다.
            }

            // 상태 Log를 감지,확인할 수 있는 메서드
            override fun onStateChange() {
                when (dataChannel.state()) {
                    DataChannel.State.OPEN -> Log.w(tagName, "onStateChange(): DataChannel is OPEN.")
                    DataChannel.State.CLOSED -> {
                        Log.w(tagName, "onStateChange(): DataChannel is CLOSED.")
//                        dataChannel.unregisterObserver() //사용자가 사용한 자원 해제할 일이 있다면..
                    }
                    else -> {Log.w(tagName, "onStateChange(): DataChannel is ELSE STATE.")}
                }
            }

            //각 피어로부터 오는 실질적인 데이터 메시지.
            override fun onMessage(buffer: DataChannel.Buffer) {
//                val message = buffer.data.toByteArray().toString(Charsets.UTF_8)
//                val message = buffer.data.toByteString().utf8()
                Log.e(tagName, "onMessage(): DataChannel 메시지옴!")

                val receivedData = buffer.data.array()
                val headerEndIndex = receivedData.lastIndexOf(58) // 찾는 문자 ':'의 ASCII 코드 값은 58
                val header = String(receivedData.copyOfRange(0, headerEndIndex))

                when {
                    header.startsWith("TEXT:", true) -> {
                        val message = String(receivedData.copyOfRange(5, receivedData.size))
                        Log.d(tagName, "DataChannel Received message: $message")
                        // 채팅 메시지 처리
                        onDataMessage?.invoke(message, null, "TEXT")
                    }
//                    header.startsWith("FILE:") -> {
//                        val fileName = header.substring(5)
//                        val fileData = receivedData.copyOfRange(headerEndIndex + 1, receivedData.size)
//                        saveFileToDownloads(fileName, fileData)
//                        onDataMessage?.invoke(fileName, fileData, "FILE")
//                    }
                    else -> {
                        Log.e(tagName, "DataChannel Invalid header: $header")
                    }
                }
            }
        })
    }




    /**
     * 사용안함. 바탕화면만 공유가능한 상태. 제대로 구현할려면, 웹소켓을 RTC_FM에 두지않고, 앱 or FM실행시 서비스에서
     * 실행하도록 해야할듯. 그래야, RTC_FM을 나가서도 RTC가 작동하게 될듯.
     */
//    fun 화면공유초기화(){
////        screenShareForResult.launch(mediaProjectionManager!!.createScreenCaptureIntent())
//        //MainActivity에 등록된 콜백으로 인텐트를 발송하여 결과를 받은뒤,
//        (context as MainActivity).register.launch(mediaProjectionManager!!.createScreenCaptureIntent())
//        //결과를 반환하는 interface 객체를 생성하여 등록하고, 그것에 접근하여 null이아니면 그대로 실행하고,
//        if((context as MainActivity).forScreenSharing != null){
//            forScreenSharing = (context as MainActivity).forScreenSharing
//
//        }else{
//            // null이면 결과반환까지 딜레이를 약간 줘서 기다린 후, 다음 작업 진행.
//            coroutineScope.launch {
//                delay(1000)
//                forScreenSharing = (context as MainActivity).forScreenSharing
//                val data = forScreenSharing!!.intentDataCalled()
//                mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data)
//                createScreenSharingPeerConnection(mediaProjection!!)
//            }
//        }
//    }

    /**
     * 화면공유를 위해 비디오 트랙을 만들고, 그것을 peerConnetion 객체의 sender에 등록하거나, 새로운 view에서 사용하도록
     * sessionManagerimple에서 목록화하기.
     */
//    private fun createScreenSharingPeerConnection(mediaProjection: MediaProjection) {
//
//        val videoCapturer = createScreenCapturer(mediaProjection)
//        val videoSource = streamFactory.factory.createVideoSource(videoCapturer!!.isScreencast).apply {
//            videoCapturer.initialize(
//                SurfaceTextureHelper.create(
//                    "ScreenShareSurfaceTextureHelperThread",
//                    streamFactory.eglBaseContext
//                ),
//                context,
//                capturerObserver
//            )
//
//            // api31이상일때는 jetpack 호환성없는 lib로, api30이하~14까지일때는 호환성 lib로 기기 화면의 크기를 받아와야함.
//            // 이때, 앱의 경계면을 받는게 아니라, 전체화면을 받아와야하기에 이런 메소드를 사용함.
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
//                val windowContext = context.createWindowContext(
//                    context.display!!,
//                    WindowManager.LayoutParams.TYPE_APPLICATION, null)
//                val projectionMetrics = windowContext.getSystemService(WindowManager::class.java).maximumWindowMetrics
//                videoCapturer.startCapture(projectionMetrics.bounds.width(), projectionMetrics.bounds.height(), 15)
//            } else {
//                val projectionMetrics = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(context as MainActivity)
//                videoCapturer.startCapture(projectionMetrics.bounds.width(), projectionMetrics.bounds.height(), 15)
//            }
//        }
//
//        val screenSharingVideoTrack = streamFactory.factory.createVideoTrack("lmsSV1", videoSource)
//        screenSharingVideoTrack.setEnabled(true)
//
//        // Add the screen sharing track to the local media stream
//        //그냥 현재 connection sender에 addTrack해주면안되나? 상관없을 것 같은데?
////        val localMediaStream = factory.createLocalMediaStream("lms")
////        localMediaStream.addTrack(screenSharingVideoTrack)
//        //별개의 역할인듯. 미디어스트림은 원격피어에게 이 트랙이 어떤 미디어스트림에 속해있는지 확인한후 그 미디어스트림
//        //을 사용할지 정할때 사용되는 역할.
////        localMediaStream.id //미디어스트림의 고유id. 생성될때 만들어짐.
//
//
//        // Replace the local video track with the screen sharing track in the PeerConnection
//        // senders를 불러오면 ndk lib로부터 갱신된 새로운 senders 리스트를 가져옴.
////        val sender = connection.senders.find { it.track()?.id() == "lmsSV1" }
//        setTrackToConnectionSender(screenSharingVideoTrack)
//
//        // If you want to switch back to the camera video track, you can call
//        // 이후 만들어둔 localVideoTrack을 밑의 sender를 찾아서 넣어주거나, 그냥 view를 하나더 추가하는 방식으로 조정.
//        // sender.setTrack(cameraVideoTrack, true) later
//    }

    fun setTrackToConnectionSender(videoTrackToSet: VideoTrack?) {
        // getSender() 에는 video, audio 등의 MediaStreamTrack객체를 보유하는 RtpSender의 List가 반환됨.
        val sender = connection.senders.find { it.track()?.kind() == "video" }
        // takeOwnership 옵션이 true일때는 호출되는 videoTrack의 소유권을 sender가 가져감. webRtc에서 가져간다는 말.
        // 그래서, set된 Track이 textureView등에서 removeSink등으로 인해 사용되지 않는 상황이 되면 자동으로 Track의
        // nativeTrack 등의 webRtc 자원은 release됨. 그래서, 그 이후에 트랙을 재사용할때 Track내의 nativeTrack이
        // 0이 되어, MediaStreamTrack has been disposed. 라는 에러가 발생하게됨. 그렇다고, 예외처리부분을 없애버리면
        // ndk 차원에서 critical error가 뜸. c++ 구현부에 맵핑되어 있던 Track point가 사라져서 그런것.
        sender?.setTrack(videoTrackToSet, false)
    }

//    private fun createScreenCapturer(mediaProjection: MediaProjection): VideoCapturer? {
//        // ScreenCapturerAndroid객체를 초기화 할려면 VideoSource객체가 필요함.
//        // 그것으로 ScreenCapturerAndroid.initialize()실행해야함.
//        val screenCapturerAndroid = ScreenCapturerAndroid(
//            mediaProjection,
//            object: MediaProjection.Callback(){
//                override fun onStop() {
//                    super.onStop()
//                    Log.e(tagName, "MediaProjection.Callback() onStop(): 권한없음?")
//                }
//            }
//        )
//        return screenCapturerAndroid
//    }





    // 수신된 파일 데이터 처리
//    val receivedFileData = mutableListOf<ByteArray>()
//    var receivedFileSize = 0L
//    val expectedFileSize = /* 예상되는 파일 크기 */
//        dataChannel.registerObserver(object : DataChannel.Observer {
//            override fun onBufferedAmountChange(previousAmount: Long) {
//                // 버퍼 크기 변경 이벤트를 처리할 수 있습니다.
//            }
//            override fun onStateChange() {
//                // 채널 상태 변경 이벤트를 처리할 수 있습니다.
//            }
//            override fun onMessage(buffer: DataChannel.Buffer) {
//                val chunk = buffer.data.array()
//                receivedFileData.add(chunk)
//                receivedFileSize += chunk.size
//                if (receivedFileSize >= expectedFileSize) {
//                    val outputFile = File(context.getExternalFilesDir(null), "receivedFile")
//                    FileOutputStream(outputFile).use { fos ->
//                        for (chunkData in receivedFileData) {
//                            fos.write(chunkData)
//                        }
//                    }
//                    Log.d("DataChannel", "File has been received completely.")
//                }
//            }
//        }


    /**
     * Used to create an offer whenever there's a negotiation that we need to process on the
     * publisher side.
     *
     * sendOffer() -> createOffer() ->    createValue { connection.createOffer(it, mediaConstraints) }
     * @return [Result] wrapper of the [SessionDescription] for the publisher.
     */
    suspend fun createOffer(): Result<SessionDescription> {
        logger.d { "[createOffer] #$typeTag, no args" }
        return createValue {
            connection.createOffer(it, mediaConstraints)
        }
    }

    /**
     * Used to create an answer whenever there's a subscriber offer.
     *
     * sendAnswer() -> createAnswer()  ->  createValue { connection.createAnswer(it, mediaConstraints) }
     * @return [Result] wrapper of the [SessionDescription] for the subscriber.
     */
    suspend fun createAnswer(): Result<SessionDescription> {
        logger.d { "[createAnswer] #$typeTag, no args" }
        return createValue {
            connection.createAnswer(it, mediaConstraints)
        }
    }

    /**
     * Used to set up the SDP on underlying connections and to add [pendingIceCandidates] to the
     * connection for listening.
     *
     * @param sessionDescription That contains the remote SDP.
     * @return An empty [Result], if the operation has been successful or not.
     */
    suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
        logger.d { "[setRemoteDescription] #$typeTag, SDP type: ${sessionDescription.type} " }
        return setValue {
            connection.setRemoteDescription(
                it,
                SessionDescription(
                    sessionDescription.type,
                    sessionDescription.description.mungeCodecs()
                )
            )
        }.also {
            //공유값인 pendingIceCandidates List를 동시접근으로부터 보호하기 위해 Mutex를 이용하여 락을 걸고 비동기 작업을 진행.
            //connection.addRtcIceCandidate(iceCandidate)의 네트워크 작업이 비동기(코루틴)이기 때문에 같은 iceCandidate 객체에
            //대한 중복 접근을 방지할 필요가 있다. 반복문이 진행되면서 비동기적인 작업에 의해 iceCandidate객체 중복추가가 될수 있기 때문.
            pendingIceMutex.withLock {
                // 이전의 offer/answer 과정중 sdp를 set하느라 상대쪽으로부터 온 ICEcandidate관련메시지를  sdp 하위에 추가하지 못했
                // 었기 때문에 pendingIceCandidate에 그동안 쌓여있고, 그것을 SDP 설정 작업이 완료되면 다 추가함.
                pendingIceCandidates.forEach { iceCandidate ->
                    logger.i { "[setRemoteDescription] #subscriber; pendingRtcIceCandidate: $iceCandidate" }
                    connection.addRtcIceCandidate(iceCandidate)
                }
                pendingIceCandidates.clear()
            }
        }
    }

    /**
     * Sets the local description for a connection either for the subscriber or publisher based on
     * the flow.
     *
     * @param sessionDescription That contains the subscriber or publisher SDP.
     * @return An empty [Result], if the operation has been successful or not.
     */
    suspend fun setLocalDescription(sessionDescription: SessionDescription): Result<Unit> {
        val sdp = SessionDescription(
            sessionDescription.type,
            sessionDescription.description.mungeCodecs()
        )
        logger.d { "[setLocalDescription] #$typeTag, SDP type: ${sessionDescription.type}" }
        //SDPUtils.kt 안에 있는 단일 메소드. Result<>객체를 반환함.
        //connection.setLocalDescription는 최종적으로 네이티브 webrtc lib에 sdp관련 옵져버인터페이스 객체와 sdp객체를 전달함.
        // 그리고 lib에서 그 옵져버인터페이스 객체를 구현하고 같이 전달한 sdp를 업데이트하면,
        // 최종적으로 자바쪽에서 업데이트된 SDP 정보를 활용할 수 있는 것.
        return setValue { connection.setLocalDescription(it, sdp) }
    }

    /**
     * Adds an [IceCandidate] to the underlying [connection] if it's already been set up, or stores
     * it for later consumption.
     *
     * @param iceCandidate To process and add to the connection.
     * @return An empty [Result], if the operation has been successful or not.
     */
    suspend fun addIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
        if (connection.remoteDescription == null) {
            logger.w { "[addIceCandidate] #$typeTag, postponed (no remoteDescription): $iceCandidate" }
            pendingIceMutex.withLock {
                pendingIceCandidates.add(iceCandidate)
            }
            return Result.failure(RuntimeException("RemoteDescription is not set"))
        }
        logger.d { "[addIceCandidate] #$typeTag, rtcIceCandidate: $iceCandidate" }
        return connection.addRtcIceCandidate(iceCandidate).also {
            logger.v { "[addIceCandidate] #$typeTag, completed: $it" }
        }
    }










    /**
     * Peer connection listeners.    ==    peerConnection.Observer의 구현부 시작 부분.
     */



    /**
     * Triggered whenever there's a new [RtcIceCandidate] for the call. Used to update our tracks
     * and subscriptions.
     *
     * [PeerConnection.Observer]의 구현부.
     * @param candidate The new candidate.
     */
    override fun onIceCandidate(candidate: IceCandidate?) {
        logger.i { "[onIceCandidate] #$typeTag, candidate: $candidate" }
        if (candidate == null) return

        onIceCandidate?.invoke(candidate, type)
    }

    /**
     * Triggered whenever there's a new [MediaStream] that was added to the connection.
     * [PeerConnection.Observer]의 구현부.
     * @param stream The stream that contains audio or video.
     */
    override fun onAddStream(stream: MediaStream?) {
        logger.i { "[onAddStream] #$typeTag, stream: $stream" }
        if (stream != null) {
            onStreamAdded?.invoke(stream)
        }
    }

    /**
     * Triggered whenever there's a new [MediaStream] or [MediaStreamTrack] that's been added
     * to the call. It contains all audio and video tracks for a given session.
     *
     * [PeerConnection.Observer]의 구현부.
     * @param receiver The receiver of tracks.
     * @param mediaStreams The streams that were added containing their appropriate tracks.
     */
    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        logger.i { "[onAddTrack] #$typeTag, receiver: $receiver, mediaStreams: $mediaStreams" }
        mediaStreams?.forEach { mediaStream ->
            logger.v { "[onAddTrack] #$typeTag, mediaStream: $mediaStream" }
            mediaStream.audioTracks?.forEach { remoteAudioTrack ->
                logger.v { "[onAddTrack] #$typeTag, remoteAudioTrack: ${remoteAudioTrack.stringify()}" }
                remoteAudioTrack.setEnabled(true)
            }
            //새로운 미디어 스트림이 있을때, 받은 mediaStream 각각의 객체를
            onStreamAdded?.invoke(mediaStream)
        }
    }

    /**
     * Triggered whenever there's a new negotiation needed for the active [PeerConnection].
     * [PeerConnection.Observer]의 구현부.
     */
    override fun onRenegotiationNeeded() {
        logger.i { "[onRenegotiationNeeded] #$typeTag, no args" }
        onNegotiationNeeded?.invoke(this, type)
    }

    /**
     * Triggered whenever a [MediaStream] was removed.
     *
     * [PeerConnection.Observer]의 구현부.
     * @param stream The stream that was removed from the connection.
     */
    override fun onRemoveStream(stream: MediaStream?) {}

    /**
     * Triggered when the connection state changes.  Used to start and stop the stats observing.
     *
     * [PeerConnection.Observer]의 구현부.
     * @param newState The new state of the [PeerConnection].
     */
    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        logger.w { "[onIceConnectionChange] #$typeTag, newState: IceConnectionState.$newState" }
        when (newState) {
            PeerConnection.IceConnectionState.CLOSED,
            PeerConnection.IceConnectionState.FAILED,
            PeerConnection.IceConnectionState.DISCONNECTED -> statsJob?.cancel()
            PeerConnection.IceConnectionState.CONNECTED ->  statsJob = observeStats()
            PeerConnection.IceConnectionState.COMPLETED -> connection.getStats(){
                Log.e(tagName, "[onIceConnectionChange] #$typeTag COMPLETED getStats: $it")
            }
            else -> Unit
        }
    }

    /**
     * @return The [RTCStatsReport] for the active connection.
     */
    fun getStats(): StateFlow<RTCStatsReport?> {
        return statsFlow
    }

    /**
     * Observes the local connection stats and emits it to [statsFlow] that users can consume.
     * 상위 코루틴을 생성함. == coroutineScope.launch
     */
    private fun observeStats() = coroutineScope.launch {
        while (isActive) {
            delay(60_000L) //60초?
            //peerConnection 객체로부터 RTCStatsReport객체(코덱,스트림,전송상태등)를 가져와서 stateFlow에 업데이트 함.
            connection.getStats {
//                logger.w { "observeStats() RTCStatsReport 관찰시작. #$typeTag, stats: $it" }
                statsFlow.value = it
            }
        }
    }

    /**
     * [PeerConnection.Observer]의 구현부.
     * Track이 추가될때마다 호출. sender, receiver 둘다 포함.
     */
    override fun onTrack(transceiver: RtpTransceiver?) {
        logger.w { "[onTrack] #$typeTag, transceiver: $transceiver" }
        onVideoTrack?.invoke(transceiver)
    }

    /**
     * Domain - [PeerConnection] and [PeerConnection.Observer] related callbacks.
     */
    override fun onRemoveTrack(receiver: RtpReceiver?) {
        logger.w { "[onRemoveTrack] #$typeTag, receiver: $receiver" }
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        logger.w { "[onSignalingChange] #$typeTag, newState: SignalingState.$newState" }
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        logger.w { "[onIceConnectionReceivingChange] #$typeTag, receiving: $receiving" }
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        logger.w { "[onIceGatheringChange] #$typeTag, newState: $newState" }
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<out org.webrtc.IceCandidate>?) {
        logger.w { "[onIceCandidatesRemoved] #$typeTag, iceCandidates: $iceCandidates" }
    }

    override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
        logger.e { "[onIceCandidateError] #$typeTag, event: ${event?.stringify()}" }
    }

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        logger.w { "[onConnectionChange] #$typeTag, newState: PeerConnectionState.$newState" }
    }

    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
        logger.w { "[onSelectedCandidatePairChanged] #$typeTag, event: $event" }
    }

    override fun onDataChannel(channel: DataChannel?){
        logger.w { "[onDataChannel] #$typeTag, DataChannel 클래스: $channel" }

        channel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {
                // 버퍼 크기 변경 이벤트를 처리할 수 있습니다.
            }

            // 상태 Log를 감지,확인할 수 있는 메서드
            override fun onStateChange() {
                when (dataChannel.state()) {
                    DataChannel.State.OPEN -> Log.w(tagName, "onStateChange(): DataChannel is OPEN.")
                    DataChannel.State.CLOSED -> {
                        Log.w(tagName, "onStateChange(): DataChannel is CLOSED.")
//                        dataChannel.unregisterObserver() //사용자가 사용한 자원 해제할 일이 있다면..
                    }
                    else -> {Log.w(tagName, "onStateChange(): DataChannel is ELSE STATE.")}
                }
            }

            //각 피어로부터 오는 실질적인 데이터 메시지.
            override fun onMessage(buffer: DataChannel.Buffer) {
//                val message = buffer.data.toByteArray().toString(Charsets.UTF_8)
//                val message = buffer.data.toByteString().utf8()
                Log.e(tagName, "onMessage(): DataChannel 메시지옴!")
                val byteBuffer = buffer.data
                val byteArray = ByteArray(byteBuffer.capacity())
                byteBuffer.get(byteArray)
//                val receivedData = buffer.data.array()
                val receivedData = byteArray
                val headerEndIndex = receivedData.indexOf(58) // 찾는 문자 ':'의 ASCII 코드 값은 58, '|'는 124
                val header = String(receivedData.copyOfRange(0, headerEndIndex + 1))

                when {
                    header.startsWith("TEXT:", true) -> {
                        val message = String(receivedData.copyOfRange(5, receivedData.size))
                        Log.d(tagName, "DataChannel Received message: $message")
                        // 채팅 메시지 처리
                        onDataMessage?.invoke(message, null, "TEXT")
                    }

//                    header.startsWith("FILE:") -> {
//                        //접두어부터 header의 끝까지를 Json문자열로 받아 파싱함.
//                        val msgEndIndex = receivedData.indexOf(124)
//                        val jin = String(receivedData.copyOfRange(5, msgEndIndex))
//                        val fileData = receivedData.copyOfRange(msgEndIndex + 1, receivedData.size)
//                        onDataMessage?.invoke(jin, fileData, "FILE")
//                    }



                    // todo  여기서 onDataMessage?.invoke(fileName, combinedData, "FILE") 의 combinedData는 미리 ChatData객체로
                    //   만들어서 보내줘야 함. 미리 receivedFileChunks 맵으로 파일을 조합하고 완료되면 보내주는 형식. 텍스트와는 다름.

                    // todo  파일 전송 후 화면 켜고 끌때 영상이 멈추는 게 아니고 화면이 꺼져있다는 이미지를 보여주게 처리하기.
                    //   화면 녹화시, 자신의 화면에만 녹화한다고 보여주는데, 다른 피어 화면에도 줌처럼 녹화한다고 보여주기.

                    header.startsWith("FILE_START:") -> {
//                        val jin = header.substring(11)
//                        val jo = JsonParser.parseString(jin).asJsonObject
//                        val fileName = jo["fileName"].asString

                        val msgEndIndex = receivedData.indexOf(124)
                        val fileName = String(receivedData.copyOfRange(11, msgEndIndex))
                        val fileData = receivedData.copyOfRange(msgEndIndex + 1, receivedData.size)

                        // 초기 파일 청크 목록을 준비합니다.
                        // todo 참고: if문으로 확인하여 같은 fileName이 존재하고 이미 전송프로세스가 진행 중일때는,
                        //   while문을 이용하여 fileName을 (1),(2),(3)...이런식으로 숫자를 덧붙여 변경하고 다시 반복확인하여
                        //   없는 번호일 경우에 그 파일명으로 최종 확정하고 맵에 넣는 방식으로 프로세스를 시작해야함.
                        receivedFileChunks[fileName] = mutableListOf()
                        receivedFileChunks[fileName]?.add(fileData)
                        Log.e(tagName, "파일 전송 - 첫 조각 받음(FILE_START).")
                    }
                    header.startsWith("FILE:") -> {
                        //접두어부터 header의 끝까지를 Json문자열로 받아 파싱함.
//                        val jin = header.substring(5)
//                        val jo = JsonParser.parseString(jin).asJsonObject
//                        val fileName = jo["fileName"].asString
//                        val fileData = receivedData.copyOfRange(headerEndIndex + 1, receivedData.size)

                        val msgEndIndex = receivedData.indexOf(124)
                        val fileName = String(receivedData.copyOfRange(5, msgEndIndex))
                        val fileData = receivedData.copyOfRange(msgEndIndex + 1, receivedData.size)

                        val combinedData = receivedFileChunks[fileName]?.add(fileData)
                        // todo  add 하곤 끝인가?
                        Log.e(tagName, "파일 전송 - 중간 조각 받는 중...(FILE).")
                    }
                    header.startsWith("FILE_END:") -> {
//                        val jin = header.substring(9)

                        val msgEndIndex = receivedData.indexOf(124)
                        val message = String(receivedData.copyOfRange(9, msgEndIndex))
                        val jo = JsonParser.parseString(message).asJsonObject
                        val fileName = jo["message"].asString
                        val fileData = receivedData.copyOfRange(msgEndIndex + 1, receivedData.size)

                        //보내는 파일의 사이즈가 chunksize보다 작은경우 byteList의 크기는 1이고 인덱스는 0이다.
                        // 보내는 when절이 인덱스를 보고 보내는 순서를 정하는데, 크기 1일때의 인덱스는 0일때의 when절만을
                        // 수행하기 때문에, 0일때 byteList size의 크기에 따른 분기점을 둬야하고, 그 분기에 따라 이곳의
                        // if문이 수행되어야함. FILE_START: 의 처리를 같이 해야하는 경우가 생김.
                        if (receivedFileChunks.containsKey(fileName)) { receivedFileChunks[fileName]?.add(fileData) }
                        else {
                            Log.e(tagName, "파일 전송 - 마지막 조각 도착(FILE_END) - byteList size 1일때 실행.")
                            receivedFileChunks[fileName] = mutableListOf()
                            receivedFileChunks[fileName]?.add(fileData)
                        }
//                        receivedFileChunks[fileName]?.add(fileData)


                        Log.e(tagName, "파일 전송 - 마지막 조각 도착(FILE_END) - receivedFileChunks[fileName].size: " +
                                "${receivedFileChunks[fileName]?.size} , 0번인덱스의 사이즈: ${receivedFileChunks[fileName]?.get(0)?.size}" +
                                ", 마지막인덱스의 사이즈: ${receivedFileChunks[fileName]?.get((receivedFileChunks[fileName]?.size)?.minus(
                                    1
                                ) ?: 0)?.size}")

                        // 파일 청크를 결합하고 저장.
                        val combinedData = receivedFileChunks[fileName]?.reduce { totalSum, bytes -> totalSum + bytes }
                        Log.e(tagName, "파일 전송 - 마지막 조각 도착(FILE_END) -combinedData.size: ${combinedData?.size}")

                        if (combinedData != null) {
                            onDataMessage?.invoke(message, combinedData, "FILE")
                        } else {
                            Log.e(tagName, "Error: File chunks not found for $fileName")
                        }

                        // ByteArray 조합 완료하면, Map에서 청크 목록을 제거.
                        receivedFileChunks.remove(fileName)
                    }
                    else -> {
                        Log.e(tagName, "DataChannel Invalid header: $header")
                    }
                }
            }
        })

    }


    /**
     * 원격 피어에 데이터를 보냄. 문자열을 바이트배열로 변환후 DataChannel.Buffer 타입으로 변환해 전송함.
     */
    fun sendMessage(text: String) {
        val header = "TEXT:"
        val message = header + JsonObject().run {
            addProperty("peerId", MyApp.userInfo.user_email)
            addProperty("nick", MyApp.userInfo.user_nick)
            addProperty("message", text)
            toString()
        }
        Log.e(tagName, "sendMessage(): DataChannel text: $text")
        if (dataChannel.state() == DataChannel.State.OPEN) {
            val buffer = DataChannel.Buffer(ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)), false)
            dataChannel.send(buffer)
            Log.e(tagName, "sendMessage(): DataChannel text Send COMPLETE: $text")
        } else {
            Log.e(tagName, "DataChannel is not open.")
        }
    }


    /**
     * 파일을 보내는 메소드.
     */
    suspend fun sendFile(fileName: String, file: ByteArray): Boolean {
        var header = "FILE_START"
        if (dataChannel.state() != DataChannel.State.OPEN) {
            Log.e(tagName, "DataChannel is not open.")
            return false
        }
        val message = JsonObject().run {
            addProperty("peerId", MyApp.userInfo.user_email)
            addProperty("nick", MyApp.userInfo.user_nick)
            addProperty("message", fileName)
            toString()
        } + "|"

        // 바이트스트림으로 연 파일(byteArray)을 내가 정한 청크 크기에 따라 ByteArray의 조각을 나누고,
        val chunkSize = 32784 //32k
        val chunks = mutableListOf<ByteArray>()
        val byteArrayInputStream = ByteArrayInputStream(file)
        val buffer = ByteArray(chunkSize)

        // 나눈 조각을 복사해 전송을 위한 ByteArray List에 차곡차곡 쌓는다.
        var bytesRead: Int //read로 읽은 buffer의 바이트수(사이즈). 위에서 32k로 지정했으니 32k일것이다.
        while (byteArrayInputStream.read(buffer).also { bytesRead = it } != -1) {
            chunks.add(buffer.copyOf(bytesRead)) //읽은 사이즈만큼의 buffer를 복사하여 리스트에 추가.
        }
        byteArrayInputStream.close()

        // 그리고, 만들어진 전송용 ByteArray를 담은 List를 header와 조합하여 원격의 피어에게 보낸다.
        chunks.forEachIndexed { i, chunk ->
            Log.w("DataChannel", "sendFile chunks size: ${chunks.size} forEach i: $i ")
            header = when(i){
                0 -> {
                    if(chunks.size != 1){
                        "FILE_START:$fileName|"
                    } else {
                        // size가 1인경우에는 받는 쪽에서 End에 관한 메시지가 가지 않기 때문에,
                        // 이렇게 예외로, 처음보낼때 완료메시지를 보내준다. 당연히 받는 쪽에는 그에 대한 예외 코드로
                        // FILE_START: 시의 코드를 FILE_END: 시에서 분기점으로 처리해줘야함.
                        "FILE_END:$message"
                    }
                }
                chunks.size-1 -> { "FILE_END:$message" }
                else -> { "FILE:$fileName|" }
            }
            val dataToSend = header.toByteArray(Charsets.UTF_8) + chunk
            val bufferToSend = DataChannel.Buffer(ByteBuffer.wrap(dataToSend), false)

            dataChannel.send(bufferToSend)

            delay(10) // 전송 간 텀을 줘서 안정성 확보
        }

        Log.w("DataChannel", "${peerId}에게 파일 전송 완료.")
        return true
    }


//    fun sendFile(fileName: String, file:ByteArray): Boolean {
//        val header = "FILE:"
//        if (dataChannel.state() != DataChannel.State.OPEN) {
//            Log.e(tagName, "DataChannel is not open.")
//            return false
//        }
//        val message = header + JsonObject().run {
//            addProperty("peerId", MyApp.userInfo.user_email)
//            addProperty("nick", MyApp.userInfo.user_nick)
//            addProperty("message", fileName)
//            toString()
//        } + "|"
//
//        //일단 보낼 파일을 파일인풋스트림으로 로드함.
////        val fileInputStream = FileInputStream(file)
////        //보낼 파일의 길이만큼의 크기를 가진 바이트배열을 만듦.
////        val byteArray = ByteArray(file.length().toInt())
////        //메모리상에 로드한 파일객체를 위에서 만든 새로운 바이트배열로 읽어 복사함.
////        fileInputStream.read(byteArray)
////        //스트림은 메모리 누수방지를 위해 꼭 닫아줘야함. 자동으로 안닫힘.
////        fileInputStream.close()
//
//        // 헤더스트링을 바이트배열로 변환하고 그것과 위에서 복사한 파일 바이트배열을 합침.
//        val dataToSend = message.toByteArray(Charsets.UTF_8) + file
//        //그리고, WebRtc의 DataChannel.Buffer타입으로 변환한 후 보냄.
//        val buffer = DataChannel.Buffer(ByteBuffer.wrap(dataToSend), false)
//        dataChannel.send(buffer)
//
//        Log.w("DataChannel", "${peerId}에게 파일 전송 완료.")
//        return true
//    }


//    fun saveFileToDownloads(fileName: String,  fileData: ByteArray) {
//        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
//            Log.e(tagName, "External storage is not available")
//            return
//        }
//
//        val downloadsDir = MyApp.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//        val file = File(downloadsDir, fileName)
//
//        try {
//            //file << 만든 디렉토리로 데이터를 write할수 있는 outputstream을 만들고,
//            val outputStream = FileOutputStream(file)
//            //받아온 데이터를 그 경로에 씀.
//            outputStream.write(fileData)
//            outputStream.close()
//            Log.i(tagName, "File saved: ${file.absolutePath}")
//        } catch (e: Exception) {
//            Log.e(tagName, "Error saving file: ${e.message}")
//        }
//    }






    override fun toString(): String =
        "StreamPeerConnection(type='$typeTag', constraints=$mediaConstraints)"

    private fun String.mungeCodecs(): String {
        return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
    }
}
