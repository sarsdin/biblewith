package com.example.androidclient.rtc.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidclient.rtc.RtcVm
import com.example.androidclient.rtc.webrtc.StandardCommand
import com.example.androidclient.rtc.webrtc.sessions.LocalWebRtcSessionManager
import com.google.gson.JsonObject

@Composable
fun CustomDialogAtRoomClick(
    selectedRoom: JsonObject,
    onConfirmClick: (JsonObject) -> Unit,
    onDismissClick: () -> Unit,
) {
//    val groupVm = LocalViewModel.current as GroupVm //모임 아이디를 서버통신에 전달하기 위한 용도.
    // viewModel을 불러온다.
    Log.e("다이얼로그방접속시", "뷰모델스토어오너: ${LocalViewModelStoreOwner.current.toString()}")
    val rtcVm = viewModel<RtcVm>() // RtcFm의 생명주기?에 매핑되어 사용할 vm을 가져옴. 생명주기 오너에 대해서는 확실치 않음.
    val sessionManager = LocalWebRtcSessionManager.current
    //setter를 이용해서 받아온 sessionManager를 set해줌.
//    rtcVm.sessionManager = sessionManager as WebRtcSessionManagerImpl

    var title by remember { mutableStateOf("") }
    var size by remember { mutableStateOf(4) }
    var pwd by remember { mutableStateOf("") }

    // 현재 다이얼로그에서 보여줄 컴포넌트들을 컨트롤하기 위한 상태값. 값에 따라 보여지는 컴포넌트들이 리컴포지션에 의해 변경됨.
    var state by remember { mutableStateOf(방접속요청상태.NONE) }

//    rtcVm.방접속시도시접속인원목록.collectAsState()
    LaunchedEffect(key1 = Unit/*rtcVm.방접속시도시접속인원목록*/) {
        rtcVm.방참가시접속인원목록.collect { value ->
            Log.e("다이얼로그방접속시", "userIds: ${value}")
            if (!value.isEmpty) {
                state = 방접속요청상태.완료
            }
        }

    }

//    LaunchedEffect(key1 = state) {
//        //상위 전달 인자로 json object를 전달함.
//        onConfirmClick(
//            JsonObject().apply {
//                addProperty("title", title)
//                addProperty("size", size)
//                addProperty("pwd", pwd)
//            }
//        )
//    }


    AlertDialog(
        onDismissRequest = { onDismissClick() },
        title = { Text(text = "참가할 방: ${selectedRoom["title"].asString}") },
        text = {
            //보냄인경우 LaunchedEffect에서 관찰하고있는 'rtcVm.방접속시도시접속인원목록'의 상태값에 따라 추후 '완료'로 바뀜.
            when(state) {
                방접속요청상태.요청보냄 -> {
                    Spacer(Modifier.size(padding))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "요청중...",
                            modifier = Modifier.fillMaxWidth().padding(start = 26.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                방접속요청상태.완료 -> {
                    Column {
                        //                buildAnnotatedString {  }
                        Text(
                            text = "방인원수: ${selectedRoom["usersCount"]} / $size",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "참가하시겠습니까?",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    Column {
                        //                buildAnnotatedString {  }
                        Text(
                            text = "방인원수: ${selectedRoom["usersCount"]} / $size",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "요청하시겠습니까?",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }


        },

        confirmButton = {
            //LaunchedEffect의 수행된 결과에 따라 state 값이 '완료'로 바뀌면 상위 컴포넌트에 서버에서 수행할 명령을 전달.
            if(state == 방접속요청상태.요청보냄){
//                TextButton(
//                    onClick = {
//                        // todo 서버와 통신하여 방접속자들 아이디 가져와야함. 차후 방장의 수락시 가져오도록 변경예정.
//                        //  현재는 그과정을 생략하고 상위컴포넌트에 명령전달.
//                        onConfirmClick( JsonObject().apply {
//                            addProperty("command", "방접속시실행")
//                            addProperty("makerId", selectedRoom["roomId"].asString)
//                            addProperty("groupId", rtcVm.groupVm.groupInfo["group_no"].asString)
//                        })
//                    }
//                ) {
//                    Text("참가")
//                }

            }else{
                TextButton(
                    onClick = {
                        // todo 서버와 통신하여 방접속자들 아이디 가져와야함. 차후 방장의 수락시 가져오도록 변경예정.
                        //   '방접속요청'으로 바꾸고 서버에서 방장이 수락하면(VideoCallSreen에서 방장만 볼수있는 컨트롤뷰를 만들고,
                        //  거기에 소켓신호가 가도록 만들어야함.) '완료'가 되도록 수신하는 부분을 추가 or 변경.
                        //  서버에서는 '방접속요청' 메소드를 만들어 수신하고, 거기서 방장에게 '접속요청'에 대한 메시지를 보내고, 다시
                        //  방장이 위에서 만들었던 뷰에서 확인하고 수락후, 다시 서버로 '수락함'이라는 메소드를 받으면 그 수락된 id에 해당하는
                        //  user에게 현재'방접속시도'에 해당하는 로직으로 여기 신호가 와서 '완료'로 바뀌게끔 해줘야함.

                        sessionManager.signalingClient.sendCommand(StandardCommand.방접속요청, JsonObject().apply {
                            addProperty("command", StandardCommand.방접속요청.name)
                            addProperty("roomId", selectedRoom["roomId"].asString)
                        })



//                        sessionManager.signalingClient.sendCommand(StandardCommand.방접속시도, JsonObject().apply {
//                            addProperty("command", StandardCommand.방접속시도.name)
//                            addProperty("roomId", selectedRoom["roomId"].asString)
//                        })

                        //userIds를 받기위해 보낸후, 상태값을 '보냄'으로 바꿔놓음. 응답데이터가 와서 그에대한 처리가 되고,
                        // 상태값이 '완료'가 될때까지 유지.
                        state = 방접속요청상태.요청보냄

                    }
                ) {
                    Text("요청")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissClick()
                }
            ) {
                Text("취소")
            }
        },
        backgroundColor = MaterialTheme.colors.background
    )
}

enum class 방접속요청상태 {
    요청보냄,
    완료,
    NONE
}