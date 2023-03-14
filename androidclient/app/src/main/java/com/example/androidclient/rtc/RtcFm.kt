package com.example.androidclient.rtc

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.androidclient.MyApp
import com.example.androidclient.databinding.RtcFmBinding
import com.example.androidclient.group.GroupVm
import io.getstream.log.Priority
import io.getstream.log.streamLog
import com.example.androidclient.rtc.ui.screens.stage.StageScreen
import com.example.androidclient.rtc.ui.screens.video.VideoCallScreen
import com.example.androidclient.rtc.ui.theme.WebrtcSampleComposeTheme
import com.example.androidclient.rtc.webrtc.SignalingClient
import com.example.androidclient.rtc.webrtc.peer.StreamPeerConnectionFactory
import com.example.androidclient.rtc.webrtc.sessions.LocalWebRtcSessionManager
import com.example.androidclient.rtc.webrtc.sessions.WebRtcSessionManager
import com.example.androidclient.rtc.webrtc.sessions.WebRtcSessionManagerImpl

/**
 *  컴포즈내에서 어디서든 사용가능하게 전역변수로 네비게이션 메소드를 할당받는 변수선언.
 */
val LocalUseNavigate: ProvidableCompositionLocal<(Int) -> Unit> = staticCompositionLocalOf { error("No UseNavigate") }
val LocalViewModel: ProvidableCompositionLocal<ViewModel> = staticCompositionLocalOf { error("No ViewModel") }
class RtcFm : Fragment() {

    val tagName = "[${this.javaClass.simpleName}]"
//    lateinit var groupVm: GroupVm


    var mbinding: RtcFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        LocalViewModel = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    lateinit var sessionManager: WebRtcSessionManager
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        mbinding = RtcFmBinding.inflate(inflater, container, false)

        requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE
        ), 0)

        //WebRtc를 제일 처음 시작하기 위해서는 웹소켓을 활용하여 시그널링 서버에 클라이언트(peer)를 등록(연결)하고
        //등록된 peer끼리 정보(SPD)를 주고 받을 수 있는 과정이 필요함.
        sessionManager = WebRtcSessionManagerImpl(
//            context = requireActivity(),
            context = MyApp.application,
            signalingClient = SignalingClient(),
            peerConnectionFactory = StreamPeerConnectionFactory(requireActivity())
        )

        binding.composeView.setContent {
            //컴포즈 내에서 네비게이션을 사용하는 콜백함수 등록.
            RtcTest(useNavigate = { dest -> findNavController().navigate(dest) } )
        }

        return binding.root
    }


    @Composable
    fun RtcTest(useNavigate: (Int) -> Unit ){
        WebrtcSampleComposeTheme {

            //뷰구성시 로컬 프로바이더로써 웹세션 매니저를 등록함. 이것은 WebRtcSessionManagerImpl 클래스안에 존재하고,
            //광역객체로써 임포트만하면 current 변수로 어디서든 접근할 수 있음.
            CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager,
                //위에서 선언한 변수에 전달받은 콜백함수를 할당해서 컴포즈 전역에서 사용가능하도록 함.
                LocalUseNavigate provides useNavigate,
                LocalViewModel provides ViewModelProvider(requireActivity()).get(GroupVm::class.java)
            ){
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
//                        color = Color.Transparent
//                        color = Color.Blue
                ) {
                    // by remember로 저장된 변수는 컴포즈에서 반응가능한 '상태값'이 되며, 이 값의 변화에 따라
                    // UI의 리컴포지션을 발생시킴. 즉, 해당 변수가 속한 컴포넌트 함수를 재실행함.
                    var onCallScreen by remember { mutableStateOf(false) }
                    //초기 값으로 세션매니저에 등록된 시그널링 클라이언트의 세션 상태값을 수집함.
                    // 이 세션 상태값이 변할때마다 컴포넌트 함수의 리컴포지션이 발생함.
                    val state by sessionManager.signalingClient.sessionStateFlow.collectAsState()

                    //그리고, State 객체를 stageScreent에 넣으며 시작함.
                    //처음에 false 라면 stageScreent 으로 가고 true 면 videoCallScreen 컴포넌트를 실행함.
                    if (!onCallScreen) {
                        streamLog(Priority.ERROR ){"비디오 스테이지 스크린 ~"}
                        //객체와 더불어 onCallScreen 값을 바꿀 수 있는 콜백도 등록.
                        // StageScreen에서 버튼 누르면 값이 true로 바뀌고, 리컴포지션 되면서 밑의 VideoCallScreen()로 넘어감.
                        // 리컴포지션 보충설명: 상태값이 변경될때 그것을 사용하고 있는 컴포넌트 함수를 다시 실행함.
                        // 하위 컴포넌트 함수일수록 변경부분이 적고, 상위 컴포넌트 함수일수록 변경점이 많아 지는 구조임.
                        //만약, 상위에서 쓰이는 전역 상태값이 있고, 그것이 변경되면 상위 컴포넌트가 다시 그려지는 결과를 가져옴.
                        StageScreen(state = state) { onCallScreen = true }

                    } else {
                        //state의 상태값이 Ready가 되면서, 버튼이 활성화되고, 그 버튼을 클릭하게 되면
                        //onCallScreen = true 의 콜백함수가 실행되면서 여기로 넘어옴.
                        //VideoCallScreen() 컴포넌트함수를 실행.
                        // 그리고, onSessionScreenReady()가 실행됨에 따라 peerConnection 객체가 초기화됨.
                        // 그후 순서
                        // localVideoTrack lazy 초기화 --> videoSource lazy 초기화
                        streamLog(Priority.ERROR ){"비디오 콜 스크린!!"}
                        VideoCallScreen()
                    }
                }
            }
        }
    }





    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




    }







    override fun onResume() {
        super.onResume()

    }



    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }














}
