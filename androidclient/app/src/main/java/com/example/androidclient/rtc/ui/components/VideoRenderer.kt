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

package com.example.androidclient.rtc.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.androidclient.MyApp
import com.example.androidclient.rtc.webrtc.sessions.LocalWebRtcSessionManager
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * Renders a single video track based on the call state.
 * 원격 or 로컬의 카메라로부터 만들어진 비디오 트랙(비디오소스랩핑)을 받아와 렌더링 뷰에서
 * 활용.
 *
 * @param videoTrack The track containing the video stream for a given participant.
 * @param modifier Modifier for styling.
 */
@Composable
fun VideoRenderer(
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier
) {
    val trackState: MutableState<VideoTrack?> = remember { mutableStateOf(null) }
    var view: VideoTextureViewRenderer? by remember { mutableStateOf(null) }
//    var view: SurfaceViewRenderer? by remember { mutableStateOf(null) }

    //받은 videoTrack 값이 변경되면 실행할 사항을 명시한다.
    //또는, 해당 컴포지션 VideoRenderer 컴포넌트를 떠날때 실행할 사항을 명시한다.
    //이때, onDispose 함수가 호출되어 특정 동작을 수행
    DisposableEffect(videoTrack) {
        onDispose {
            //VideoRenderer 컴포넌트를 떠날때 비디오트랙객체도 정리해야함.
            cleanTrack(view, trackState)
        }
    }


    val sessionManager = LocalWebRtcSessionManager.current
    AndroidView(
        //factory내에서 활용되는 타입은 View타입으로 제네릭이 강제되어있음.
        factory = { context ->
            //context는 AndroidView내부의 변수인 val context = LocalContext.current  << 이것을 받아서
            //AndroidView를 초기화 하는데 사용한다. LocalContext.current는 안드로이드 내에서 사용가능한 context를 가져온다.
            // 즉, 현재 view가 실행될 Activity의 context 인듯.
            // texture view 를 반환하는 함수를 AndroidView()의 factory 인자로써 넣어준다.
            VideoTextureViewRenderer(context).apply {
                // Video Texture view 와 그것을 활용하는 랜더러를 초기화 하기 위해 컨텍스트와
                // 랜더링 이벤트를 생성하여 등록함.
                init(
                    sessionManager.peerConnectionFactory.eglBaseContext,
                    object : RendererCommon.RendererEvents {
                        override fun onFirstFrameRendered() = Unit

                        override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) = Unit
                    }
                )

                setupVideo(trackState, videoTrack, this)
                //마지막으로 VideoRenderer의 상태값인 view에
                // 이 Video Texture view 객체를 넣어주고, 그것을 팩토리에 반환함으로 뷰를완성해줌.
                view = this
            }
//            SurfaceViewRenderer(context).apply {
//                init(
//                    sessionManager.peerConnectionFactory.eglBaseContext,
//                    object : RendererCommon.RendererEvents {
//                        override fun onFirstFrameRendered() = Unit
//
//                        override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) = Unit
//                    }
//                )
//                setupVideo(trackState, videoTrack, this)
//                //마지막으로 VideoRenderer의 상태값인 view에
//                // 이 Video Texture view 객체를 넣어주고, 그것을 팩토리에 반환함으로 뷰를완성해줌.
//                view = this
//            }

        },
        //레이아웃이 inflate 된후 호출되는 콜백. factory때 실행한 setupVideo()를 다시 실행함.
        //무슨 의미인지는 모르겠지만, 리컴포지션 시 factory를 재실행하지 않고 사용하기 위함인듯.
        update = { v -> setupVideo(trackState, videoTrack, v) },
        modifier = modifier
    )
}

/**
 * 비디오 트랙을 사용하는 VideoTextureViewRenderer 뷰와 VideoTrack 객체를 담고 있는
 * 상태값을 받아와 비디오트랙의 싱크를 끊고 상태값에서 제거해줌.
 */
private fun cleanTrack(
    view: VideoTextureViewRenderer?,
//    view: SurfaceViewRenderer?,
    trackState: MutableState<VideoTrack?>
) {
    view?.let { trackState.value?.removeSink(it) }
    trackState.value = null
}


/**
 * 현재 수신받은 비디오 트랙 (원격 or 로컬 둘중하나임. 하나는 플로팅렌더러에 쓰일것임)
 * 을 이 컴포넌트의 trackState 값에 넣어줌. 그리고, 현재 렌더러 뷰와 싱크 시작.
 */
private fun setupVideo(
    trackState: MutableState<VideoTrack?>,
    track: VideoTrack,
    renderer: VideoTextureViewRenderer
//    renderer: SurfaceViewRenderer
) {
    //현재 들어오고 있는 비디오 트랙(수신중인)이 이전에 상태값에 저장했던 객체와 같은 객체면
    // 비디오 셋업을 할 필요가 없음. 그래서 그냥 종료.
    if (trackState.value == track) {
        return
    }

    //그게 아니라면 일단 트랙을 비우고,
    cleanTrack(renderer, trackState)

    //현재 수신받은 비디오 트랙 (원격 or 로컬 둘중하나임. 하나는 플로팅렌더러에 쓰일것임)
    //을 이 컴포넌트의 trackState 값에 넣어줌. 그리고, 현재 렌더러 뷰와 싱크 시작.
    trackState.value = track
    track.addSink(renderer)
}
