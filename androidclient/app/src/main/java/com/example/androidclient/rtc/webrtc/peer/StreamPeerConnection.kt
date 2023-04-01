package com.example.androidclient.rtc.webrtc.peer

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.window.layout.WindowMetricsCalculator
import com.example.androidclient.MyApp
import com.example.androidclient.home.MainActivity
import com.example.androidclient.rtc.webrtc.utils.addRtcIceCandidate
import com.example.androidclient.rtc.webrtc.utils.createValue
import com.example.androidclient.rtc.webrtc.utils.setValue
import com.example.androidclient.rtc.webrtc.utils.stringify
import com.google.gson.JsonObject
import io.getstream.log.taggedLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
    val peerId: String,
    val context: Context,
    val streamFactory : StreamPeerConnectionFactory,
    private val coroutineScope: CoroutineScope,
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
    private val onVideoTrack: ((RtpTransceiver?) -> Unit)?,
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


    /**
     * 화면공유 관련 변수 및 콜백. mediaProjectionManager 객체를 시스템으로부터 받아온다.
     */
    val mediaProjectionManager = getSystemService(context, MediaProjectionManager::class.java)
    var mediaProjection : MediaProjection? =  null
//    val screenShareForResult = (context as MainActivity)
    var forScreenSharing: MainActivity.ResultMediaProjectionForRTCscreenSharing? = null

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
                    header.startsWith("FILE:") -> {
                        val fileName = header.substring(5)
                        val fileData = receivedData.copyOfRange(headerEndIndex + 1, receivedData.size)
                        saveFileToDownloads(fileName, fileData)
                        onDataMessage?.invoke(fileName, fileData, "FILE")
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
    suspend fun sendFile(file: File) {
        val header = "FILE:"
        if (dataChannel.state() != DataChannel.State.OPEN) {
            Log.e(tagName, "DataChannel is not open.")
            return
        }

        //일단 보낼 파일을 파일인풋스트림으로 로드함.
        val fileInputStream = FileInputStream(file)
        //보낼 파일의 길이만큼의 크기를 가진 바이트배열을 만듦.
        val byteArray = ByteArray(file.length().toInt())
        //메모리상에 로드한 파일객체를 위에서 만든 새로운 바이트배열로 읽어 복사함.
        fileInputStream.read(byteArray)
        //스트림은 메모리 누수방지를 위해 꼭 닫아줘야함. 자동으로 안닫힘.
        fileInputStream.close()

        // 헤더스트링을 바이트배열로 변환하고 그것과 위에서 복사한 파일 바이트배열을 합침.
        val dataToSend = header.toByteArray() + byteArray
        //그리고, WebRtc의 DataChannel.Buffer타입으로 변환한 후 보냄.
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(dataToSend), false)
        dataChannel.send(buffer)




//        val chunkSize = 16384 // 각 청크의 크기 설정 (16KB)
//        val fileInputStream = FileInputStream(file)
//        val buffer = ByteArray(chunkSize)
//        while (fileInputStream.read(buffer) > 0) {
//            dataChannel.send(DataChannel.Buffer(ByteBuffer.wrap(buffer), false))
//            delay(10) // 네트워크 혼잡을 피하기 위한 간단한 지연
//        }
//        fileInputStream.close()
//        Log.d("DataChannel", "File has been sent completely.")


    }


    fun saveFileToDownloads(fileName: String,  fileData: ByteArray) {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            Log.e(tagName, "External storage is not available")
            return
        }

        val downloadsDir = MyApp.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            //file << 만든 디렉토리로 데이터를 write할수 있는 outputstream을 만들고,
            val outputStream = FileOutputStream(file)
            //받아온 데이터를 그 경로에 씀.
            outputStream.write(fileData)
            outputStream.close()
            Log.i(tagName, "File saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(tagName, "Error saving file: ${e.message}")
        }
    }


    /**
     * 사용안함. 바탕화면만 공유가능한 상태. 제대로 구현할려면, 웹소켓을 RTC_FM에 두지않고, 앱 or FM실행시 서비스에서
     * 실행하도록 해야할듯. 그래야, RTC_FM을 나가서도 RTC가 작동하게 될듯.
     */
    fun 화면공유초기화(){
//        screenShareForResult.launch(mediaProjectionManager!!.createScreenCaptureIntent())
        //MainActivity에 등록된 콜백으로 인텐트를 발송하여 결과를 받은뒤,
        (context as MainActivity).register.launch(mediaProjectionManager!!.createScreenCaptureIntent())
        //결과를 반환하는 interface 객체를 생성하여 등록하고, 그것에 접근하여 null이아니면 그대로 실행하고,
        if((context as MainActivity).forScreenSharing != null){
            forScreenSharing = (context as MainActivity).forScreenSharing

        }else{
            // null이면 결과반환까지 딜레이를 약간 줘서 기다린 후, 다음 작업 진행.
            coroutineScope.launch {
                delay(500)
                forScreenSharing = (context as MainActivity).forScreenSharing
                val data = forScreenSharing!!.intentDataCalled()
                mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data)
                createScreenSharingPeerConnection(mediaProjection!!)
            }
        }
    }

    /**
     * 화면공유를 위해 비디오 트랙을 만들고, 그것을 peerConnetion 객체의 sender에 등록하거나, 새로운 view에서 사용하도록
     * sessionManagerimple에서 목록화하기.
     */
    private fun createScreenSharingPeerConnection(mediaProjection: MediaProjection) {

        val videoCapturer = createScreenCapturer(mediaProjection)
        val videoSource = streamFactory.factory.createVideoSource(videoCapturer!!.isScreencast).apply {
            videoCapturer.initialize(
                SurfaceTextureHelper.create(
                    "ScreenShareSurfaceTextureHelperThread",
                    streamFactory.eglBaseContext
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
                videoCapturer.startCapture(projectionMetrics.bounds.width(), projectionMetrics.bounds.height(), 15)
            } else {
                val projectionMetrics = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(context as MainActivity)
                videoCapturer.startCapture(projectionMetrics.bounds.width(), projectionMetrics.bounds.height(), 15)
            }
        }

        val screenSharingVideoTrack = streamFactory.factory.createVideoTrack("lmsSV1", videoSource)
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
        val sender = connection.senders.find { it.track()?.kind() == "video" }
        sender?.setTrack(screenSharingVideoTrack, true)

        // If you want to switch back to the camera video track, you can call
        // 이후 만들어둔 localVideoTrack을 밑의 sender를 찾아서 넣어주거나, 그냥 view를 하나더 추가하는 방식으로 조정.
        // sender.setTrack(cameraVideoTrack, true) later
    }

    private fun createScreenCapturer(mediaProjection: MediaProjection): VideoCapturer? {
        // ScreenCapturerAndroid객체를 초기화 할려면 VideoSource객체가 필요함.
        // 그것으로 ScreenCapturerAndroid.initialize()실행해야함.
        val screenCapturerAndroid = ScreenCapturerAndroid(
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
                val headerEndIndex = receivedData.lastIndexOf(58) // 찾는 문자 ':'의 ASCII 코드 값은 58
                val header = String(receivedData.copyOfRange(0, headerEndIndex))

                when {
                    header.startsWith("TEXT:", true) -> {
                        val message = String(receivedData.copyOfRange(5, receivedData.size))
                        Log.d(tagName, "DataChannel Received message: $message")
                        // 채팅 메시지 처리
                        onDataMessage?.invoke(message, null, "TEXT")
                    }
                    header.startsWith("FILE:") -> {
                        val fileName = header.substring(5)
                        val fileData = receivedData.copyOfRange(headerEndIndex + 1, receivedData.size)
                        saveFileToDownloads(fileName, fileData)
                        onDataMessage?.invoke(fileName, fileData, "FILE")
                    }
                    else -> {
                        Log.e(tagName, "DataChannel Invalid header: $header")
                    }
                }
            }
        })

    }








    override fun toString(): String =
        "StreamPeerConnection(type='$typeTag', constraints=$mediaConstraints)"

    private fun String.mungeCodecs(): String {
        return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
    }
}
