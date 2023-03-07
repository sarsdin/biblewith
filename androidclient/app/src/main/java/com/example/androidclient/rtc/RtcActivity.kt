//package com.example.androidclient.rtc
//
//import android.Manifest
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.runtime.CompositionLocalProvider
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.fragment.app.Fragment
//import io.getstream.log.Priority
//import io.getstream.log.streamLog
//import com.example.androidclient.rtc.ui.screens.stage.StageScreen
//import io.getstream.webrtc.sample.compose.ui.screens.video.VideoCallScreen
//import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
//import com.example.androidclient.rtc.webrtc.SignalingClient
//import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
//import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
//import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManager
//import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManagerImpl
//
//class RtcFragment : Fragment() {
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 0)
//
//        //WebRtc를 제일 처음 시작하기 위해서는 시그널링 서버에 웹소켓을 활용하여 peer를 통해 클라이언트를 등록하고(연결하고)
//        //
//        val sessionManager: WebRtcSessionManager = WebRtcSessionManagerImpl(
//            context = this,
//            signalingClient = SignalingClient(),
//            peerConnectionFactory = StreamPeerConnectionFactory(this)
//        )
//
//        setContent {
//            WebrtcSampleComposeTheme {
//                CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager) {
//                    // A surface container using the 'background' color from the theme
//                    Surface(
//                        modifier = Modifier.fillMaxSize(),
//                        color = MaterialTheme.colorScheme.background
////                        color = Color.Transparent
////                        color = Color.Blue
//                    ) {
//                        var onCallScreen by remember { mutableStateOf(false) }
//                        val state by sessionManager.signalingClient.sessionStateFlow.collectAsState()
//
//                        if (!onCallScreen) {
//                            streamLog(Priority.ERROR ){"비디오 스테이지 ~"}
//                            StageScreen(state = state) { onCallScreen = true }
//                        } else {
//                            streamLog(Priority.ERROR ){"비디오 콜 스크린!!"}
//                            VideoCallScreen()
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
