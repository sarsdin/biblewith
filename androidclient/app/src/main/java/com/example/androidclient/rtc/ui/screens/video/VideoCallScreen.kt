package com.example.androidclient.rtc.ui.screens.video
import android.os.Looper
import android.util.Log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.os.postDelayed
import com.example.androidclient.R
import com.example.androidclient.rtc.LocalUseNavigate
import com.example.androidclient.rtc.ui.components.VideoRenderer
import com.example.androidclient.rtc.webrtc.StandardCommand
import com.example.androidclient.rtc.webrtc.sessions.LocalWebRtcSessionManager
import com.example.androidclient.rtc.webrtc.sessions.WebRtcSessionManagerImpl
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.logging.Handler
import kotlin.math.ceil
import kotlin.math.sqrt


/**
 * 비디오뷰가 나오는 컴포넌트. 클라들의 상태가 ready에서 (이후 ready상태에 이르는 것은 방목록화면이 보이는 상태와 동일할것임.)
 * offer / answer 를 주고 받으면 ice가 되어 영상이 송출된다.
 */
@Composable
fun VideoCallScreen() {

    //MainActivity에서 등록한 CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager)의 값을 불러올 수 있음.
    val sessionManager = LocalWebRtcSessionManager.current
    val navigate = LocalUseNavigate.current
    val sessionState = sessionManager.signalingClient.sessionStateFlow

    //LaunchedEffect()는 코루틴 스코프 중 하나인데, key1 값을 상태값으로 가지고 있으며,
    //여기 할당된 변수의 변화를 감지하고 스코프내 코드를 재실행한다.
    LaunchedEffect(key1 = Unit/*sessionState*/) {
        //비디오 콜 스크린시 처음으로 peerConnection 변수가 초기화됨.
        sessionManager.onSessionScreenReady()
    }


    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = Color.Cyan)
    ) {
        var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }

        //원격에서 onAddTrack()등으로 onVideoTrack()이 실행되면 이변수가 emit되어, 이곳이 리컴포지션이 되야 정상임.
//        val remoteVideoTrackState by sessionManager.remoteVideoTrackFlow.collectAsState(null)
//        val remoteVideoTrack = remoteVideoTrackState
        val remoteVideoTracks by sessionManager.remoteVideoTracks.collectAsState(emptyList())

        val localVideoTrackState by sessionManager.localVideoTrackFlow.collectAsState(null)
        val localVideoTrack = localVideoTrackState



        /**
         * 카메라와 마이크 on/off 현황을 저장하는 객체의 상태를 저장.
         */
        var callMediaState by remember { mutableStateOf(CallMediaState()) }
//        val si =  (sessionManager as WebRtcSessionManagerImpl)

        if(localVideoTrack == null){
            (sessionManager as WebRtcSessionManagerImpl).reCreateLocalVideoTrack()
        }




        //원격 피어의 비디오트랙이 송출되어 왔다면, 비디오 렌더링 컴포넌트를 시작함.
        Log.e("VideoCallScreen()", "VideoRenderer 실행 조건 미통과 remoteVideoTrack: $remoteVideoTracks")
        val trackCount = remoteVideoTracks.size
        if (trackCount > 0) {
            Log.e("VideoCallScreen()", "VideoRenderer 실행 조건 통과 remoteVideoTrack: $remoteVideoTracks")

            //Ui 관련 변수
            val columns = ceil(sqrt(trackCount.toFloat())).toInt()
            val rows = ceil(trackCount.toFloat() / columns).toInt()

            GridBox(
                rows = rows,
                columns = columns,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val videoTrack = remoteVideoTracks.getOrNull(index)

                if (videoTrack != null) {
                    VideoRenderer(
                        videoTrack = videoTrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(color = Color.Blue)
                            .padding(5.dp)
                            .border(width = 2.dp, color = Color.Red, shape = RoundedCornerShape(5.dp))
                    )
                }
            }


        }


//            VideoRenderer(
//                videoTrack = remoteVideoTrack,
//                modifier = Modifier
//                    .fillMaxSize()
//                    .onSizeChanged { parentSize = it }
//            )


        //나의 카메라로부터 비디오트랙이 만들어졌고, 내 카메라 상태가 On 이라면,
        // 플로팅 비디오 렌더링 컴포넌트를 시작함.
        Log.e("VideoCallScreen()", "FloatingVideoRenderer 실행 조건 미통과 localVideoTrack: $localVideoTrack")
        if ((localVideoTrack != null) && callMediaState.isCameraEnabled) {
            Log.e("VideoCallScreen()", "FloatingVideoRenderer 실행 조건 통과 localVideoTrack: $localVideoTrack")
            FloatingVideoRenderer(
                modifier = Modifier
                    .size(width = 150.dp, height = 180.dp) //height = 210.dp
                    .clip(RoundedCornerShape(16.dp))
                    .align(Alignment.TopEnd),
                videoTrack = localVideoTrack,
                parentBounds = parentSize,
                paddingValues = PaddingValues(0.dp)
            )
        }

//        val activity = (LocalContext.current as? Activity)


        VideoCallControls(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            callMediaState = callMediaState,
            onCallAction = {
                when (it) {
                    is CallAction.ToggleMicroPhone -> {
                        val enabled = callMediaState.isMicrophoneEnabled.not()
                        callMediaState = callMediaState.copy(isMicrophoneEnabled = enabled)
                        sessionManager.enableMicrophone(enabled)
                    }
                    is CallAction.ToggleCamera -> {
                        val enabled = callMediaState.isCameraEnabled.not()
                        callMediaState = callMediaState.copy(isCameraEnabled = enabled)
                        sessionManager.enableCamera(enabled)
                    }
                    CallAction.FlipCamera -> sessionManager.flipCamera()
                    CallAction.LeaveCall -> {

                        CoroutineScope(Dispatchers.Default).launch {
                            sessionManager.signalingClient.sendCommand(
                                StandardCommand.접속해제,
                                JsonObject().apply {
                                    addProperty("command", "접속해제")
                                }
                            )

                            sessionManager.disconnect()
    //                        sessionManager.isDisconnected(true)
                            android.os.Handler(Looper.getMainLooper()).postDelayed(Runnable {
                                navigate(R.id.action_global_groupInFm)
                            }, 500)
                        }



                    }

                }
            }
        )



    }
}



@Composable
fun GridBox(
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit
) {
    Column(modifier = modifier) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                repeat(columns) { column ->
                    val index = row * columns + column

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        content(index)
                    }
                }
            }
        }
    }
}
