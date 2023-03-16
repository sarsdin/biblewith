package com.example.androidclient.rtc

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.RtcFmBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.rtc.ui.screens.stage.StageScreen
import com.example.androidclient.rtc.ui.screens.video.VideoCallScreen
import com.example.androidclient.rtc.ui.theme.WebrtcSampleComposeTheme
import com.example.androidclient.rtc.webrtc.SignalingClient
import com.example.androidclient.rtc.webrtc.StandardCommand
import com.example.androidclient.rtc.webrtc.peer.StreamPeerConnectionFactory
import com.example.androidclient.rtc.webrtc.sessions.LocalWebRtcSessionManager
import com.example.androidclient.rtc.webrtc.sessions.WebRtcSessionManager
import com.example.androidclient.rtc.webrtc.sessions.WebRtcSessionManagerImpl
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 *  컴포즈내에서 어디서든 사용가능하게 전역변수로 네비게이션 메소드를 할당받는 변수선언.
 */
val LocalUseNavigate: ProvidableCompositionLocal<(Int) -> Unit> = staticCompositionLocalOf { error("No UseNavigate") }
val LocalViewModel: ProvidableCompositionLocal<ViewModel> = staticCompositionLocalOf { error("No ViewModel") }
class RtcFm : Fragment() {

    val tagName = "[${this.javaClass.simpleName}]"
    lateinit var groupVm: GroupVm


    var mbinding: RtcFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
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
            signalingClient = SignalingClient(groupVm),
            peerConnectionFactory = StreamPeerConnectionFactory(requireActivity())
        )

        binding.composeView.setContent {
            //컴포즈 내에서 네비게이션을 사용하는 콜백함수 등록.
            RtcTest(useNavigate = { dest -> findNavController().navigate(dest) } )
        }

        return binding.root
    }

    enum class ScreenState {
        ROOM_LIST, STAGE_SCREEN, VIDEO_CALL_SCREEN
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
                    color = MaterialTheme.colors.background
//                        color = Color.Transparent
//                        color = Color.Blue
                ){
                    val navigate = LocalUseNavigate.current

//                    val rtcVm: RtcVm = viewModel(viewModelStoreOwner = this)
//                    val roomList by rtcVm.roomList.collectAsState() // ViewModel의 roomList 사용하기
//                    val currentScreen by rtcVm.currentScreen.collectAsState() // ViewModel의 currentScreen 사용하기

                    // 방목록 담는 변수: 소켓으로부터 받아온 JsonArray()
                    val roomList by sessionManager.signalingClient.roomList.collectAsState()
                    val roomL = roomList
                    //현재 스크린의 위치를 담는 변수.
                    val _currentScreen by  sessionManager.signalingClient.currentScreen.collectAsState()
                    val currentScreen = _currentScreen
//                    //새로 만든 화면 확장용 변수
//                    var currentScreen by remember { mutableStateOf(ScreenState.ROOM_LIST) }
//                    //웹소켓으로부터 받은 방목록 변수
//                    val roomList by remember { mutableStateOf(emptyList<String>()) }

                    //초기 값으로 세션매니저에 등록된 시그널링 클라이언트의 세션 상태값을 수집함.
                    // 이 세션 상태값이 변할때마다 컴포넌트 함수의 리컴포지션이 발생함.(active, ready, creating, impossible, offline)
                    val state by sessionManager.signalingClient.sessionStateFlow.collectAsState()

                    //여기서 어떻게든 소켓으로부터 signalingClient의 init에서 받은 room list를 가져와야함.
                    //되도록이면 저위의 rtcVm안에 roomList에 넣으면 좋음.
                    //?? 그냥 viewModel말고 sessionManager.signalingClient에 새 변수 넣고 사용하면 안되나?? 그러면 될듯??;;


                    when (currentScreen) {
                        ScreenState.ROOM_LIST -> {
                            RoomList(
                                rooms = roomL,
                                onRoomClick = { selectedRoom ->
                                    // 원하는 방을 선택한 후 처리할 작업을 여기에 추가합니다.
                                    // 예를 들어, selectedRoom을 사용하여 StageScreen 및 VideoCallScreen 컴포넌트에 전달할 수 있습니다.
                                    sessionManager.signalingClient.setCurrentScreen(ScreenState.STAGE_SCREEN)
//                                    sessionManager.signalingClient.setCurrentScreen(ScreenState.VIDEO_CALL_SCREEN)
                                    //방클릭시 서버소켓에 방접속 명령보내야하지 않을까?
                                    sessionManager.signalingClient.sendCommand(StandardCommand.방접속,
                                        JsonObject().apply {
                                            addProperty("command", "방접속")
                                            addProperty("makerId", selectedRoom["roomId"].asString)
                                            addProperty("groupId", groupVm.groupInfo["group_no"].asString)
                                        }
                                    )
                                },
                                onBackPressed = {
                                    // 뒤로 가기 기능을 처리하는 로직을 여기에 추가.
                                    navigate(R.id.action_global_groupInFm)
                                },
                                onCreateRoom = { title, size, pwd ->
                                    // 방 만들기 기능을 처리하는 로직을 여기에 추가.
                                    sessionManager.signalingClient.sendCommand(StandardCommand.방만들기,
                                        JsonObject().apply {
                                            addProperty("command", "방만들기")
                                            addProperty("title", title)
                                            addProperty("size", size)
                                            addProperty("pwd", pwd)
                                            addProperty("groupId", groupVm.groupInfo["group_no"].asString)
                                        }
                                    )
                                }
                            )
                        }
                        ScreenState.STAGE_SCREEN -> {
                            StageScreen(state = state) {
                                sessionManager.signalingClient.setCurrentScreen(ScreenState.VIDEO_CALL_SCREEN)
                            }
                        }
                        ScreenState.VIDEO_CALL_SCREEN -> {
                            VideoCallScreen()
                        }
                    }




                }
            }
        }
    }



    /**
     * 리사이클러뷰 컴포넌트. 여기서 String이 웹소켓에서 받은 room하나의 정보를 담고있는 객체타입이 되야함.
     */
    @Composable
    fun RoomList(rooms: JsonArray/*List<String>*/,
         onRoomClick: (JsonObject) -> Unit,
         onBackPressed: () -> Unit,
         onCreateRoom: (String, Int, String) -> Unit
    ) {

        val showDialog = remember { mutableStateOf(false) }

        fun showCreateRoomDialog() {
            showDialog.value = true
        }

        fun hideCreateRoomDialog() {
            showDialog.value = false
        }

        //rooms자체는 signalingClient의 리스너에서 JsonObject()로써 온전히 받기때문에 그 객체안의 속성이 있는지 확인해야함.
        Log.e(tagName, "rooms: $rooms")
        val roomL = rooms.map{ it.asJsonObject }
        Log.e(tagName, "roomL: $roomL")

        Column {
            TopAppBar(
                title = { Text("RTC 방목록") },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateRoomDialog() }) {
                        Icon(Icons.Filled.Add, contentDescription = "방만들기")
                    }
                },
                backgroundColor = MaterialTheme.colors.background,
                modifier = Modifier.height(50.dp), elevation = 1.dp
            )


            LazyColumn {
                itemsIndexed(roomL) { index, room ->
                    Text(
                        text = "$index ${room["title"]}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRoomClick(room) }
                            .padding(16.dp)
                    )
                }
            }

            if (showDialog.value) {
                var title by remember { mutableStateOf("") }
                var size by remember { mutableStateOf(4) }
                var pwd by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { hideCreateRoomDialog() },
                    title = { Text(text = "방 만들기") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = title, // 방 제목 입력란의 상태 변수
                                onValueChange = {
                                    title = it
                                }, // 상태 변수 업데이트
                                label = { Text("방 제목") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = size.toString(), // 인원 수 설정 입력란의 상태 변수
                                onValueChange = {input ->
                                    if (input.all { it.isDigit() }) {
                                        size = input.toInt()
                                    }
//                                    size = it.toInt()
                                }, // 상태 변수 업데이트
                                label = { Text("인원 수") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {})
                            )
                            OutlinedTextField(
                                value = pwd, // 비밀번호 입력란의 상태 변수
                                onValueChange = {pwd = it}, // 상태 변수 업데이트
                                label = { Text("비밀번호") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    // 클릭시 방을 만드는: 소켓으로 서버에 '방만들기' 명령내림.
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onCreateRoom(title, size, pwd)
                                hideCreateRoomDialog()
                            }
                        ) {
                            Text("확인")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                hideCreateRoomDialog()
                            }
                        ) {
                            Text("취소")
                        }
                    },
                    backgroundColor = MaterialTheme.colors.background
                ) // AlertDialog end
            }


        } //Column end


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




/*


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


*/