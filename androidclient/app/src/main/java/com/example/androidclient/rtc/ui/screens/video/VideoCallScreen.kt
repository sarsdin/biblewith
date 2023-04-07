package com.example.androidclient.rtc.ui.screens.video

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.rtc.LocalFm
import com.example.androidclient.rtc.LocalUseNavigate
import com.example.androidclient.rtc.RtcVm
import com.example.androidclient.rtc.ui.components.JoinRequestDialog
import com.example.androidclient.rtc.ui.components.VideoRenderer
import com.example.androidclient.rtc.ui.screens.ChatPanel
import com.example.androidclient.rtc.ui.screens.ExpandableFAB
import com.example.androidclient.rtc.webrtc.StandardCommand
import com.example.androidclient.rtc.webrtc.peer.StreamPeerConnection
import com.example.androidclient.rtc.webrtc.sessions.ChatData
import com.example.androidclient.rtc.webrtc.sessions.LocalWebRtcSessionManager
import com.example.androidclient.rtc.webrtc.sessions.WebRtcSessionManagerImpl
import com.example.androidclient.util.FileHelperV2
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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
    val rtcFm = LocalFm.current
//    val sessionState = sessionManager.signalingClient.sessionStateFlow

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun setOutputPath() {
        val filename = generateFileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = rtcFm.requireActivity().contentResolver
            contentValues = ContentValues()
            contentValues!!.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Record")
            contentValues!!.put(MediaStore.Video.Media.TITLE, filename)
            contentValues!!.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            contentValues!!.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            mUri = resolver!!.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            //FILE NAME SHOULD BE THE SAME
            rtcFm.hbRecorder.setFileName(filename)
            rtcFm.hbRecorder.setOutputUri(mUri)
        } else {
            createFolder()
            rtcFm.hbRecorder.setOutputPath("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)}/Record")
        }
    }


    //화면 녹화 전용 변수
    val result = remember { mutableStateOf<Intent?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK){
                result.value = it.data
                setOutputPath()
                rtcFm.hbRecorder.startScreenRecording(result.value, Activity.RESULT_OK)
            }
        }
    )


    val rtcVm = viewModel<RtcVm>()
    rtcVm.sessionManager = sessionManager as WebRtcSessionManagerImpl

    //LaunchedEffect()는 코루틴 스코프 중 하나인데, key1 값을 상태값으로 가지고 있으며,
    //여기 할당된 변수의 변화를 감지하고 스코프내 코드를 재실행한다.
    LaunchedEffect(key1 = Unit/*sessionState*/) {
        //비디오 콜 스크린시 처음으로 peerConnection 변수가 초기화됨.
        sessionManager.onSessionScreenReady()
    }

    //VideoCallScreen 전용 코루틴
    val rememberCoroutineScope = rememberCoroutineScope()



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.DarkGray)
    ) {
//        var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }

        //원격에서 onAddTrack()등으로 onVideoTrack()이 실행되면 이변수가 emit되어, 이곳이 리컴포지션이 되야 정상임.
//        val remoteVideoTrackState by sessionManager.remoteVideoTrackFlow.collectAsState(null)
//        val remoteVideoTrack = remoteVideoTrackState
        val remoteVideoTracks by sessionManager.remoteVideoTracks.collectAsState(emptyList())

        val localVideoTrack by sessionManager.localVideoTrackFlow.collectAsState(null)
//        val localVideoTrack = localVideoTrackState

        val chatMessages by rtcVm.chatMessages.collectAsState(emptyList())



        /**
         * 카메라와 마이크 on/off 현황을 저장하는 객체의 상태를 저장.
         */
        var callMediaState by remember { mutableStateOf(CallMediaState()) }
//        val si =  (sessionManager as WebRtcSessionManagerImpl)

        // 로컬 비디오 트랙이 null 이면 재생성함.
//        if(localVideoTrack == null){
//            (sessionManager as WebRtcSessionManagerImpl).reCreateLocalVideoTrack()
//        }

        val 다이얼로그보여주기 = remember{ mutableStateOf("") }
        fun 다이얼로그닫기(){
            다이얼로그보여주기.value = ""
        }

        LaunchedEffect(key1 = Unit){
            //방장에게접속요청자목록의 값을 관찰하고있다가 변화가 감지되면, 아이콘의 모습을 바꿈.
            rtcVm.방장에게접속요청자목록.collect{
                callMediaState = if (it.size() > 0){
                    callMediaState.copy(isRequestList = true)
                } else {
                    callMediaState.copy(isRequestList = false)
                }
            }
        }


        //원격 피어의 비디오트랙이 송출되어 왔다면, 비디오 렌더링 컴포넌트를 시작함.
        Log.e("VideoCallScreen()", "VideoRenderer 실행 조건 미통과 remoteVideoTrack: $remoteVideoTracks")
        // todo localVideoTrack 용 +1임. 나중에 화면공유까지 되면 localVideoTracks로 바꿔야함.
        val trackCount = remoteVideoTracks.size + 1
        if (trackCount > 0) {
            Log.e("VideoCallScreen()", "VideoRenderer 실행 조건 통과 remoteVideoTrack: $remoteVideoTracks")

            //Ui 관련 변수
//            var columns = ceil(sqrt(trackCount.toFloat())).toInt() //제곱근: 예)2의 제곱근= 1.414.. , ceil: 올림 예)1.414 -> 2
//            var rows = ceil(trackCount.toFloat() / columns).toInt()

            var rows = ceil(sqrt(trackCount.toFloat())).toInt() //제곱근: 예)2의 제곱근= 1.414.. , ceil: 올림 예)1.414 -> 2
            var columns = ceil(trackCount.toFloat() / rows).toInt()

            var 높이우선여부 = false
            val ratio = if(trackCount == 2){
                높이우선여부 = false
                columns = 1
                rows = 2    //화면에 표시할 트랙이 2개뿐이면 1열 2행을 꽉찬 고정화면으로 보여주기.
                0.8f
            }else if(trackCount >= 5){
                높이우선여부 = true
                columns = 2
                0.5f
            } else {
                1f
            }

            GridBox(
                trackCount = trackCount,
                rows = rows,
                columns = columns,
                modifier = Modifier.fillMaxSize()
            ) { index ->

                val videoTrack = if(index == 0) {
                    //첫번째 칸에는 localVideoTrack를 표시해야함.
                    localVideoTrack
                } else {
                    remoteVideoTracks.getOrNull(index - 1)
                }


                if (videoTrack != null) {
                    val peerId = videoTrack.peerInfo["peerId"] ?: "나의 화면"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
//                            .aspectRatio(ratio, 높이우선여부)
                            .background(color = Color.Black)
                            .padding(5.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(5.dp)
                            )
                    ) {
//                        if ( !callMediaState.isCameraEnabled && index == 0){
//                            Text(
//                                text = "카메라가 없어요..",
//                                modifier = Modifier
//                                    .align(Alignment.TopCenter)
//                                    .padding(4.dp),
//                                color = Color.White,
//                                fontSize = 18.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//
//                            Image(
//                                painter = painterResource(id = R.drawable.ddam),
//                                contentDescription = "이미지 없음",
//                                modifier = Modifier.fillMaxSize().align(Alignment.Center)
//                            )
//
//                            Text(
//                                text = peerId.substringBefore("@"),
//                                modifier = Modifier
//                                    .align(Alignment.BottomStart)
//                                    .padding(4.dp),
//                                color = Color.White,
//                                fontSize = 18.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//
//
//                        } else {
//                        }

                        VideoRenderer(
                            videoTrack = videoTrack,
                            modifier = Modifier.fillMaxSize()
                        )

                        Text(
                            text = peerId.substringBefore("@"),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )


                    }

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
//        Log.e("VideoCallScreen()", "FloatingVideoRenderer 실행 조건 미통과 localVideoTrack: $localVideoTrack")
//        if ((localVideoTrack != null) && callMediaState.isCameraEnabled) {
//            Log.e("VideoCallScreen()", "FloatingVideoRenderer 실행 조건 통과 localVideoTrack: $localVideoTrack")
//            FloatingVideoRenderer(
//                modifier = Modifier
//                    .size(width = 150.dp, height = 180.dp) //height = 210.dp
//                    .clip(RoundedCornerShape(16.dp))
//                    .align(Alignment.TopEnd),
//                videoTrack = localVideoTrack,
//                parentBounds = parentSize,
//                paddingValues = PaddingValues(0.dp)
//            )
//        }

//        val activity = (LocalContext.current as? Activity)


        VideoCallControls(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            callMediaState = callMediaState,
            onCallAction = {

                //CallAction이 sealed class 가 아니라면, CallAction 클래스에서 자식 클래스가 무엇이 있는지 알지 못해
                // when 절에서 else{} 문을 추가하라는 컴파일 에러가 뜸. 자식클래스가 하나 더 추가되면, 그에 대한 처리를
                // 해줘야 하기에 컴파일 에러가 뜨는게 개발자 입장에선 당연히 편하다. 일종의 오류 감지를 위한 장치.
                when (it) {
                    is CallAction.RequestList -> {
                        // todo rtvVm.RequestList 목록(방장에게접속요청자목록)의 size를 보고 0이면 false, 1이상이면 true로 되어 아이콘이 달라지게끔.
                        //  클릭시 진행사항 작성. 이벤트시에만 상태값을 변경하는 코딩방식을 따라야함.
                        다이얼로그보여주기.value = "요청자목록"
                        if (rtcVm.방장에게접속요청자목록.value.size() > 0){
//                            val enabled = callMediaState.isRequestList.not()
                            callMediaState = callMediaState.copy(isRequestList = true)
                        } else {
                            callMediaState = callMediaState.copy(isRequestList = false)
                        }

                    }
                    is CallAction.ToggleMicroPhone -> {
                        val enabled = callMediaState.isMicrophoneEnabled.not()
                        //현재 data class 객체를 복사하되, 안의 속성의 값은 변경하여 복사하기.
                        callMediaState = callMediaState.copy(isMicrophoneEnabled = enabled)
                        sessionManager.enableMicrophone(enabled)
                    }
                    is CallAction.ToggleCamera -> {
                        val enabled = callMediaState.isCameraEnabled.not()
                        callMediaState = callMediaState.copy(isCameraEnabled = enabled)
                        sessionManager.enableCamera(enabled)
                    }
                    is CallAction.Record -> {
                        val enabled = callMediaState.isRecordEnabled.not()
                        callMediaState = callMediaState.copy(isRecordEnabled = enabled)
                        // todo  Fm 객체를 참조해 Record 객체를 가져와서 recording start 한다.
                        //   클릭상태가 true일때는 녹화시작, false 일때는 녹화 중지.
                        if (enabled){

                            // todo 녹화를 껏을때 mediaProjection을 종료하게 하는 것이 문제임.
                            //  이것때문에 화면 공유 중지를 누르면 에러뜸.
                            //화면 공유등에서 이미 재사용할 intent를 받아와 할당해있다면 재사용하고,
                            if(rtcVm.sessionManager.intentForPermission != null){
                                result.value = rtcVm.sessionManager.intentForPermission
                                setOutputPath()
                                rtcFm.hbRecorder.startScreenRecording(result.value, Activity.RESULT_OK)

                            } else {
                                //아니면 할당받아 사용함.
                                val mediaProjectionManager = getSystemService(rtcFm.requireContext(), MediaProjectionManager::class.java)
                                val permissionIntent = mediaProjectionManager?.createScreenCaptureIntent()
                                launcher.launch(permissionIntent)
                            }

                        } else {
                            // todo 녹화를 껏을때 mediaProjection을 종료하게 하는 것이 문제임. hbRecord에는
                            //   mediaProjection을 stop()
                            //  이것때문에 화면 공유 중지를 누르면 에러뜸. 객체안에 resetAll()이라는 것이 있는데,
                            //   서비스와 mediaProjection를 중지시킴. 이렇게 했을때, 화면공유는 mediaProjection객체를
                            //   못찾아 중지되는 듯한데, 그래서 해당 videoTrack을 못찾아 에러뜸.
                            rtcFm.hbRecorder.stopScreenRecording()
                        }
                    }

                    CallAction.FlipCamera -> sessionManager.flipCamera()
                    CallAction.LeaveCall -> {

                        sessionManager.signalingClient.sendCommand(
                            StandardCommand.접속해제,
                            JsonObject().apply {
                                addProperty("command", StandardCommand.접속해제.name)
                            }
                        )

                        sessionManager.disconnect()
//                        sessionManager.isDisconnected(true)
                        android.os.Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            navigate(R.id.action_global_groupInFm)
                        }, 500)

//                        rememberCoroutineScope.launch {  } //이건 주의할 부분이 컴포넌트의 생명주기에 따른다는 부분.
//                        CoroutineScope(Dispatchers.Default).launch {
//                        }



                    }

                }
            }
        ) //VideoCall control end



        //방장일때 참가요청자가 있는지 확인해야함.
        if ((rtcVm.접속한방정보읽기["makerId"].asString == MyApp.userInfo.user_email) && callMediaState.isRequestList
            && 다이얼로그보여주기.value == "요청자목록"
        ){
            JoinRequestDialog { 다이얼로그닫기() }
        }




        /**
         * 방의 다른 peer들에게 채팅 메시지를 보낼때 실행됨.
         */
        fun onSendMessage(message: String) {
            val chatData = ChatData(
                message = message,
                type = "TEXT",
                userId = MyApp.userInfo.user_email, // peerId 설정.
                nick = MyApp.userInfo.user_nick // 닉네임 설정.
            )

            // 메시지를 전송하는 로직을 작성.
            rtcVm.sessionManager.getPeerConnections().forEach { (peerId, peerConnection) ->
                // 각 PeerConnection의 DataChannel을 통해 chatData를 전송.
                if(peerId != MyApp.userInfo.user_email){
                    Log.e("RTC채팅전송", "peerId에게 전송: ${peerId}")
                    peerConnection.sendMessage(message)
                }
            }

            // 메시지를 채팅 목록에 추가.
            rtcVm.addChatMessage(chatData)
        }

        //채팅 패널 컴포넌트. 나의 채팅과 다른 피어들의 채팅 메시지를 보여주고, 채팅을 보낼 수 있다.
        //선언된 함수를 콜백으로 적을때는 람다 표현으로 작성해야함.
        ChatPanel(chatMessages, onSendMessage = { message -> onSendMessage(message) } )






        fun onSendFile(fileName :String, file: ByteArray) {
            val chatData = ChatData(
                message = fileName,
                type = "FILE",
                userId = MyApp.userInfo.user_email, // peerId 설정.
                nick = MyApp.userInfo.user_nick, // 닉네임 설정.
            )

            // 메시지를 채팅 목록에 추가.
            rtcVm.addChatMessage(chatData)

            var sentCountToPeer = 0
            // 메시지를 전송하는 로직을 작성.
            rtcVm.sessionManager.getPeerConnections().forEach { (peerId, peerConnection) ->
                // 각 PeerConnection의 DataChannel을 통해 chatData를 전송.
                if(peerId != MyApp.userInfo.user_email){
                    Log.e("RTC채팅 File 전송", "peerId에게 전송 11: ${peerId}")
                    rememberCoroutineScope.launch {

                        if (peerConnection.sendFile(fileName, file)){
                            // 진행률 업데이트
                            sentCountToPeer++

                            val progress = (sentCountToPeer.toFloat() / rtcVm.sessionManager.getPeerConnections().size.toFloat()) * 100
                            chatData.progress.emit(progress)

                            Log.e("RTC채팅 File 전송", "진행률 업데이트 - peerId에게 전송: ${peerId}, progress:$progress, sentCountToPeer:$sentCountToPeer")
                        }
                    }
                    Log.e("RTC채팅 File 전송", "peerId에게 전송 22: ${peerId}")
                }
            }

        }

//        val context = LocalContext.current
        val context = LocalFm.current.context
//        var fileResultMap = remember { mutableMapOf<String,File>() }
        val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
//                val path = UtilsFile().getFullPathFromContentUri(context, uri)
//                val file = File(path)
//                fileResultMap[file.name] = file
                val helper = FileHelperV2()
                val name = helper.getFileNameFromUri(context!!, uri)
                Log.e("VideoCallScreen", "fileLauncher name: ${name} , uri: $uri")
                val byteArray = helper.readFileFromUri(context, uri)

                onSendFile(name!!, byteArray!!)
            }
        }

        ExpandableFAB(
            onSendFile = {
                //todo file 전송시 실행할 부분. 탐색기로 파일을 선택하고 그 파일객체를 보내는 로직을 짜야함.
                //  파일객체를 청크단위로 쪼개고, 반복문으로 보내는 메서드(sendFile)를 실행해야함.
                //  그리고, 반복문으로 보내는 와중에 파일객체를 쪼갠 리스트의 size를 전체로 잡고, 현재index / size *100
                //  으로 % 를 구하고 그 Float을 ChatPanel 내부의 progressbar에 전달해서 업데이트되게 해야함.
                //  그럴려면, chatMessages list에 추가될 chatData객체 내부의 progress 변수를 할당하고 그것을 이용해서
                //  ChatItem 컴포넌트가 리컴포지션이 되게 해야함.
                //   수정 - 파일객체를 쪼개는 것에서 보내기 완료된 peer의 수가 현재index를 대체함.
                fileLauncher.launch("*/*")
            },

        )


    } //Box end
}

/**
 * 녹화시 저장될 파일의 이름을 설정함.
 * Generate a timestamp to be used as a file name
 */
fun generateFileName(): String? {
    val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    val curDate = Date(System.currentTimeMillis())
    return formatter.format(curDate).replace(" ", "")
}

/**
 * 안드로이드 9(Api28) 이하의 버전일때 로컬저장소에 녹화 파일을 저장하기 위해 사용하는 메서드임.
 * Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
 * This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
 */
fun createFolder() {
    val f1 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HBRecorder");
    if (!f1.exists()) {
        if (f1.mkdirs()) {
            Log.i("Folder ", "created");
        }
    }
}

//For Android 10> we will pass a Uri to HBRecorder
//This is not necessary - You can still use getExternalStoragePublicDirectory
//But then you will have to add android:requestLegacyExternalStorage="true" in your Manifest
//IT IS IMPORTANT TO SET THE FILE NAME THE SAME AS THE NAME YOU USE FOR TITLE AND DISPLAY_NAME
var resolver: ContentResolver? = null
var contentValues: ContentValues? = null
var mUri: Uri? = null


@Composable
fun GridBox(
    trackCount: Int,
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit
) {
    Column(modifier = modifier) {
        repeat(rows) { row ->
            if(trackCount == 3){
                if(row == 0){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        repeat(1) { column ->
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

                }else{
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        repeat(2) { column -> //row:1 , columns:2  column row의 i는 무조건 0부터 시작임.
                            val index = (row * columns)-1 + column  // index는 실질적으로 1, 2가 되야함.

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

            } else {
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
}



//suspend fun 파일분할전송(
//    file: File,
//    peerConnection: StreamPeerConnection
//) {
//    val chunkSize = 32784 //32k
//    val chunks = mutableListOf<ByteArray>()
//    val fileInputStream = FileInputStream(file)
//    val buffer = ByteArray(chunkSize)
//
//    var bytesRead: Int
//    while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
//        chunks.add(buffer.copyOf(bytesRead))
//    }
//    fileInputStream.close()
//
//    //                        val totalChunks = chunks.size
//    //                        var chunksSent = 0
//
//    for (chunk in chunks) {
//        val header = "FILE:"
//        val dataToSend = header.toByteArray() + chunk
//        val bufferToSend = DataChannel.Buffer(ByteBuffer.wrap(dataToSend), false)
//
//        peerConnection.sendFile(bufferToSend)
//        // 진행률 업데이트
//        //                            chunksSent++
//        //                            val progress = (chunksSent.toFloat() / totalChunks.toFloat()) * 100
//        //                            chatData.progress = progress
//
//        //                            onProgressUpdate(progress.toInt())
//        delay(10) // 전송 간 텀을 줘서 안정성 확보
//    }
//}