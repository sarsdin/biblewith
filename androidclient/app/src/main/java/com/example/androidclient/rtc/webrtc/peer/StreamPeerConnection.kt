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

package com.example.androidclient.rtc.webrtc.peer
import android.util.Log

import io.getstream.log.taggedLogger
import com.example.androidclient.rtc.webrtc.utils.addRtcIceCandidate
import com.example.androidclient.rtc.webrtc.utils.createValue
import com.example.androidclient.rtc.webrtc.utils.setValue
import com.example.androidclient.rtc.webrtc.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription

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
    private val coroutineScope: CoroutineScope,
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
    private val onVideoTrack: ((RtpTransceiver?) -> Unit)?
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

    init {
        logger.i { "<init> #$typeTag, mediaConstraints: $mediaConstraints" }
    }

    /**
     * Initialize a [StreamPeerConnection] using a WebRTC [PeerConnection].
     *
     * @param peerConnection The connection that holds audio and video tracks.
     */
    fun initialize(peerConnection: PeerConnection) {
        logger.d { "[initialize] #$typeTag, peerConnection: $peerConnection" }
        this.connection = peerConnection
    }

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
        logger.d { "[setRemoteDescription] #$typeTag, answerSdp: ${sessionDescription.stringify()}" }
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
        logger.d { "[setLocalDescription] #$typeTag, Sdp: ${sessionDescription.stringify()}" }
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
    }








    override fun toString(): String =
        "StreamPeerConnection(type='$typeTag', constraints=$mediaConstraints)"

    private fun String.mungeCodecs(): String {
        return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
    }
}