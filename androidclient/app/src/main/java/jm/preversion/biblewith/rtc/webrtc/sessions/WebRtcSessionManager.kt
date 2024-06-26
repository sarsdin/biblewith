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

package jm.preversion.biblewith.rtc.webrtc.sessions

import jm.preversion.biblewith.rtc.webrtc.SignalingClient
import jm.preversion.biblewith.rtc.webrtc.peer.StreamPeerConnectionFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.VideoTrack

interface WebRtcSessionManager {

    val signalingClient: SignalingClient

    val peerConnectionFactory: StreamPeerConnectionFactory

    val localVideoTrackFlow: SharedFlow<VideoTrack>

//    val remoteVideoTracks: MutableStateFlow<MutableMap<String, VideoTrack>>
    val remoteVideoTracks: StateFlow<List<VideoTrack>>
//    val remoteVideoTracks: SharedFlow<List<VideoTrack>>

    /**
     * WebRtcSessionManagerImpl 클래스안에 구현되어 있음.
     * 기기의 화면이 준비됐을때 peerConnection객체를 by lazy에 의해 초기화함.
     */
    fun onSessionScreenReady()

    fun flipCamera()

    fun enableMicrophone(enabled: Boolean)

    fun enableCamera(enabled: Boolean)

    fun disconnect()
    fun isDisconnected(): Boolean

    fun isDisconnected(execute:Boolean): Boolean
}
