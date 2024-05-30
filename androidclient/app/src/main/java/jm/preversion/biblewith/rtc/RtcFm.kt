package jm.preversion.biblewith.rtc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import jm.preversion.biblewith.R
import jm.preversion.biblewith.databinding.RtcFmBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.rtc.ui.components.CustomDialog
import jm.preversion.biblewith.rtc.ui.components.CustomDialogAtRoomClick
import jm.preversion.biblewith.rtc.ui.screens.stage.StageScreen
import jm.preversion.biblewith.rtc.ui.screens.video.VideoCallScreen
import jm.preversion.biblewith.rtc.ui.theme.RtcComposeTheme
import jm.preversion.biblewith.rtc.webrtc.SignalingClient
import jm.preversion.biblewith.rtc.webrtc.StandardCommand
import jm.preversion.biblewith.rtc.webrtc.peer.StreamPeerConnectionFactory
import jm.preversion.biblewith.rtc.webrtc.sessions.LocalWebRtcSessionManager
import jm.preversion.biblewith.rtc.webrtc.sessions.WebRtcSessionManager
import jm.preversion.biblewith.rtc.webrtc.sessions.WebRtcSessionManagerImpl
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import es.dmoral.toasty.Toasty

/**
 *  컴포즈내에서 어디서든 사용가능하게 전역변수로 네비게이션 메소드를 할당받는 변수선언.
 */
val LocalUseNavigate: ProvidableCompositionLocal<(Int) -> Unit> = staticCompositionLocalOf { error("No UseNavigate") }
//val LocalViewModel: ProvidableCompositionLocal<ViewModel> = staticCompositionLocalOf { error("No ViewModel") }
val LocalFm: ProvidableCompositionLocal<RtcFm> = staticCompositionLocalOf { error("No Fm") }
class RtcFm : Fragment(), HBRecorderListener {

    val tagName = "[${this.javaClass.simpleName}]"
    lateinit var groupVm: GroupVm


    var mbinding: RtcFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    //Init HBRecorder : 화면 녹화 객체 초기화
    lateinit var hbRecorder :HBRecorder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        hbRecorder = HBRecorder(requireActivity(), this)
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
            context = requireActivity(),
//            context = MyApp.application,
            rtcFm = this,
            signalingClient = SignalingClient(groupVm),
            peerConnectionFactory = StreamPeerConnectionFactory(requireActivity())
        )

        binding.composeView.setContent {
            //컴포즈 내에서 네비게이션을 사용하는 콜백함수 등록.
            Rtc(useNavigate = { dest -> findNavController().navigate(dest) } )
        }

        return binding.root
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        // todo  pip mode를 감별하는 state값을 하나 선언하고 여기서 그 값의 변화를 일으킨다.
        //  그리고, 변경할 컴포넌트에서  state를 구독하는 변수를 선언하고 그에 따른 동작을 작성한다.
    }

    enum class ScreenState {
        ROOM_LIST, STAGE_SCREEN, VIDEO_CALL_SCREEN, AT_ROOM_CLICKED, 방장접속
    }

    /**
     * RtcVm 객체를 Fm의 멤버변수로 등록해줌.
     */
    lateinit var rtcVm : RtcVm

    @Composable
    fun Rtc(useNavigate: (Int) -> Unit){

        RtcComposeTheme {
            //뷰구성시 로컬 프로바이더로써 웹세션 매니저를 등록함. 이것은 WebRtcSessionManagerImpl 클래스안에 존재하고,
            //광역객체로써 임포트만하면 current 변수로 어디서든 접근할 수 있음.
            CompositionLocalProvider(
                //위에서 선언한 변수에 전달받은 콜백함수를 할당해서 컴포즈 전역에서 사용가능하도록 함.
                LocalUseNavigate provides useNavigate,
                LocalWebRtcSessionManager provides sessionManager,
//                LocalViewModel provides groupVm
                LocalFm provides  this
            ){
                //상위 컴포넌트에서 하위 컴포넌트에 사용될 여러 프로바이더를 설정해줌.
                val navigate = LocalUseNavigate.current
                val sessionManager = LocalWebRtcSessionManager.current
//                val rtcVm = LocalViewModel.current as RtcVm
                rtcVm = viewModel<RtcVm>()
                rtcVm.sessionManager = sessionManager as WebRtcSessionManagerImpl
                rtcVm.groupVm = groupVm
                rtcVm.rtcFm = this

                Log.e(tagName, "뷰모델스토어오너: ${LocalViewModelStoreOwner.current.toString()}")


                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // A surface container using the 'background' color from the theme
                    color = MaterialTheme.colors.background
//                        color = Color.Transparent
//                        color = Color.Blue
                ){

//                    val rtcVm: RtcVm = viewModel(viewModelStoreOwner = this)
//                    val roomList by rtcVm.roomList.collectAsState() // ViewModel의 roomList 사용하기
//                    val currentScreen by rtcVm.currentScreen.collectAsState() // ViewModel의 currentScreen 사용하기

                    // 방목록 담는 변수: 소켓으로부터 받아온 JsonArray()
                    val roomL by sessionManager.signalingClient.roomList.collectAsState()
//                    val roomL = roomList
                    //현재 스크린의 위치를 담는 변수.
                    val currentScreen by  sessionManager.signalingClient.currentScreen.collectAsState()
//                    val currentScreen = _currentScreen
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

//                    var _selectedRoom by remember { mutableStateOf(JsonObject()) }

                    when (currentScreen) {
                        ScreenState.ROOM_LIST -> {
                            RoomList (
                                rooms = roomL,
                                onRoomEntrance = { 방접속 ->
//                                    // 원하는 방을 선택한 후 처리할 작업을 여기에 추가합니다.
//                                    // 예를 들어, selectedRoom을 사용하여 StageScreen 및 VideoCallScreen 컴포넌트에 전달할 수 있습니다.
//                                    sessionManager.signalingClient.setCurrentScreen(ScreenState.STAGE_SCREEN)
////                                    sessionManager.signalingClient.setCurrentScreen(ScreenState.VIDEO_CALL_SCREEN)
//                                    //방클릭시 서버소켓에 방접속 명령보내야하지 않을까?
//                                    sessionManager.signalingClient.sendCommand(StandardCommand.방접속,
//                                        JsonObject().apply {
//                                            addProperty("command", "방접속")
//                                            addProperty("makerId", selectedRoom["roomId"].asString)
//                                            addProperty("groupId", groupVm.groupInfo["group_no"].asString)
//                                        }
//                                    )
                                    //
//                                    sessionManager.signalingClient.setCurrentScreen(ScreenState.AT_ROOM_CLICKED)
//                                    _selectedRoom = selectedRoom

                                    //CustomDialogAtRoomClick 다이얼로그에서 요청 후 참가를 클릭할때 실행됨. 서버로 전달할 명령임.
                                    // 서버에서는 해당 방의 접속원들의 아이디를 보내줘야하고, 그것을 이용해 VideoCallScreen에서는
                                    // onSessionScreenReady() 를 실행함. << 즉, OFFER를 시작함.
                                    // 이때, sendOffer()를 반복문으로 돌릴때 쓰이는 것이 방금 전달받은 방접속원들 아이디 리스트임.
                                    sessionManager.signalingClient.sendCommand(StandardCommand.방접속, 방접속)

                                    // todo 그냥 화면 바꾸란 명령을 내리면, 위에서 서버로의 비동기 통신이 완료되지 않아서 데이터가
                                    //  없을 가능성도 있음. 그렇다면 해야할 것?  데이터를 받았는지 확인하고 화면 전환 명령을 해야함.
                                    //  받았을때 변동되는 상태값에 따라 (명령을내려)바뀌게 해야함.
                                    //  sessionManager.signalingClient.setCurrentScreen의 값을 받는 위치에서
                                    //  ScreenState.VIDEO_CALL_SCREEN 으로 변경해주면 이곳의 currentScreen 상태값이 바뀌니
                                    //  알아서 전환될 것임.
//                                    sessionManager.signalingClient.setCurrentScreen(ScreenState.VIDEO_CALL_SCREEN)

                                },
                                onBackPressed = {
                                    // 뒤로 가기 기능을 처리하는 로직을 여기에 추가.
                                    navigate(R.id.action_global_groupInFm)
                                },
                                onCreateRoom = { jo ->
                                    // 방 만들기 기능을 처리하는 로직을 여기에 추가.
                                    sessionManager.signalingClient.sendCommand(StandardCommand.방만들기,
                                        JsonObject().apply {
                                            addProperty("command", "방만들기")
                                            addProperty("title", jo["title"].asString)
                                            addProperty("size", jo["size"].asString)
                                            addProperty("pwd", jo["pwd"].asString)
                                            addProperty("groupId", groupVm.groupInfo["group_no"].asString)
                                        }
                                    )


                                }
                            )
                        }
//                        ScreenState.AT_ROOM_CLICKED -> {
//                            CustomDialogAtRoomClick(
//                                selectedRoom = _selectedRoom,
//                                onConfirmClick = { 방접속 ->
//                                    sessionManager.signalingClient.sendCommand(StandardCommand.방접속, 방접속)
//                                } ,
//                                onDismissClick = {    }
//                            )
//                        }

                        ScreenState.STAGE_SCREEN -> {
                            StageScreen(state = state) {
                                //두번째 인자인 onJoinCall() 구현부.
                                sessionManager.signalingClient.setCurrentScreen(ScreenState.VIDEO_CALL_SCREEN)
                            }
                        }

                        ScreenState.VIDEO_CALL_SCREEN -> {
                            VideoCallScreen()
                        }
                        else -> {}
                    }




                }
            }
        }
    }



    /**
     * 리사이클러뷰 컴포넌트. 여기서 String이 웹소켓에서 받은 room하나의 정보를 담고있는 객체타입이 되야함.
     */
    @Composable
    fun RoomList(
        rooms: JsonArray/*List<String>*/,
        onRoomEntrance: (JsonObject) -> Unit,
        onBackPressed: () -> Unit,
        onCreateRoom: (JsonObject) -> Unit
    ) {

        //'방접속시도', '방만들기' 등 이변수의 값에 따라 해당하는 다이얼로그를 보여주는 용도
        val showDialog = remember { mutableStateOf("") }

        fun showRoomDialog(다이얼로그종류:String) {
            showDialog.value = 다이얼로그종류
        }

        fun hideRoomDialog() {
            showDialog.value = ""
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
                    IconButton(onClick = { showRoomDialog("방만들기") }) {
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
                            //방목록을 클릭하면,
                            .clickable { showRoomDialog("방접속시도") }
                            .padding(16.dp)
                    )

                    // 방접속시도에 관한 다이얼로그가 뜸.
                    if(showDialog.value == "방접속시도"){
                        CustomDialogAtRoomClick(
                            selectedRoom = room ,
                            onConfirmClick = onRoomEntrance,
                            onDismissClick = { hideRoomDialog() }
                        )
                    }
                }
            }


            // 다이얼로그(방만들기용)
            if (showDialog.value == "방만들기") {
                //상위 컴포넌트에서 전달받은 구현부 콜백을 그대로 다이얼로그 컴포넌트에 전달해줌.
                // 전달할 콜백은 2가지.
                CustomDialog( onCreateRoom, { hideRoomDialog() })
            }


        } //Column end


    }


    var register = this.registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult(), object : ActivityResultCallback<ActivityResult> {

            override fun onActivityResult(result: ActivityResult) {
                val resultCode = result.resultCode
                val data = result.data
                if (resultCode == Activity.RESULT_OK) {
                    assert(data != null)
                    Log.e(tagName, "onActivityResult() 콜백 호출됨. Intent data 리턴!? $data")
                    (sessionManager as WebRtcSessionManagerImpl).화면공유초기화(data!!)
                }
            }
        }
    )






    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




    }







    override fun onResume() {
        super.onResume()

    }



    override fun onDestroyView() {
        super.onDestroyView()
        //이미 세션메니저가 종료되었는지 확인해야함. nullpoint Exception 대비.
//        if(sessionManager.isDisconnected()) sessionManager.disconnect()
        mbinding = null
    }

    override fun HBRecorderOnStart() {
        Log.w(tagName, "HBRecorderOnStart()")
        Toasty.success(requireActivity(), "녹화 시작").show()
    }

    override fun HBRecorderOnComplete() {
        Log.w(tagName, "HBRecorderOnComplete()")
        Toasty.success(requireActivity(), "녹화 완료").show()
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        Log.e(tagName, "HBRecorderOnError() errorCode: $errorCode, reason: $reason")
    }

    override fun HBRecorderOnPause() {
        Log.w(tagName, "HBRecorderOnPause()")
    }

    override fun HBRecorderOnResume() {
        Log.w(tagName, "HBRecorderOnResume()")
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