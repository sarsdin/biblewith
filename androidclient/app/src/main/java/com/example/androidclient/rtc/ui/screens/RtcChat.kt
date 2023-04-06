package com.example.androidclient.rtc.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidclient.MyApp
import com.example.androidclient.rtc.RtcVm
import com.example.androidclient.rtc.ui.theme.반투명검정
import com.example.androidclient.rtc.webrtc.sessions.ChatData
import com.example.androidclient.util.FileHelperV2
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun ChatPanel(
    chatMessages: List<ChatData>,
    onSendMessage: (String) -> Unit ) {
    var isChatVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)

    ) {
        //채팅 리스트뷰 및 입력창 컴포넌트
        if (isChatVisible) {
            ChatList(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 58.dp),
                chatMessages
            )
            ChatInput(modifier = Modifier.align(Alignment.BottomStart),
                onSendMessage
            )
        }

        // 채팅창 보이기 or 감추기 버튼
        IconButton(
            onClick = { isChatVisible = !isChatVisible },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-70).dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(반투명검정)
        ) {
            Icon(
                imageVector = if (isChatVisible) Icons.Default.Close else Icons.Rounded.List,
                contentDescription = if (isChatVisible) "Close Chat" else "Open Chat",
                tint = Color.LightGray
            )
        }
    }
}


@Composable
fun ChatList(
    modifier: Modifier = Modifier,
    chatMessages: List<ChatData>) {

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .fillMaxWidth()
            .heightIn(0.dp, 350.dp),
        userScrollEnabled = true
    ) {
        items(chatMessages) { chatData ->
            ChatItem(chatData)
        }
    }

    LaunchedEffect(chatMessages) {
        if(chatMessages.isNotEmpty()){
            launch {
                delay(50) // 애니메이션을 위한 딜레이 추가
                listState.animateScrollToItem(chatMessages.size - 1) // 가장 최근 항목으로 스크롤
            }
        }
    }

}

@Composable
fun ChatItem(
//    modifier: Modifier = Modifier,
    chatData: ChatData) {

    val rtcVm = viewModel<RtcVm>()

    //todo  chatData객체를 remember로 state값으로 만들고, 그것의 변화(file progress)를 감지하여 리컴포지션이 일어나게 해야함.
    //   FILE일때만 state를 적용하게 실험해보기.
//    var chatDataFile by remember { mutableStateOf(0f) }
    val chatDataFile = chatData.progress.collectAsState()
//    var chatDataFile = 0f
//    LaunchedEffect(key1 = chatData.progress){
//        launch {
//            chatDataFile = chatData.progress
//        }
//    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        if (chatData.type == "TEXT") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text("${chatData.nick}: ", color = Color.LightGray)
                Text(chatData.message, color = Color.White)
            }

        } else if (chatData.type == "FILE") {
            // 파일 처리 로직을 여기에 작성.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${chatData.nick}: ", color = Color.LightGray)
                Text(
                    text = chatData.message,
                    color = Color.White)
                Spacer(modifier = Modifier.size(16.dp))

                //todo 여기 progressbar작성

                if (chatDataFile.value/*.value.progress*/ >= 1f || chatData.userId != MyApp.userInfo.user_email){
                    //todo 다운로드 버튼 나오게 하여, 클릭시 다운로드 처리가 진행되게 함.
                    Log.e("RtcChat", "(if) 다운로드 받기 버튼 활성화됨: ${chatDataFile/*.value.progress*/}")
//                    rtcVm
                    Text(
                        modifier = Modifier.clickable {
                            //todo 로컬에 다운받기 메서드 실행. rtcVm에 다운받기 메서드 넣거나 여기 메서드 포함하기.
                            rtcVm.fileHelper.saveFileToDownloads(rtcVm.rtcFm.requireContext(), chatData.message, chatData.file!!)
                            Toasty.success(rtcVm.rtcFm.requireContext(), "파일 다운로드 완료").show()
                        },
                        text = "받기",
                        color = Color.Green)
                } else {
                    Log.e("RtcChat", "(else) chatDataFile.value.progress: ${chatDataFile/*.value.progress*/}")
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        progress = chatDataFile.value/*.value.progress*/,
                        backgroundColor = Color.Green
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "${chatDataFile.value}%",
                        color = Color.Green)

                }

            }
        }
    }
}

@Composable
fun ChatInput(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 채팅 입력창
        TextField(
            value = message,
            onValueChange = { newMessage -> message = newMessage },
            modifier = Modifier
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(11.dp)),
//                .background(Color.White, RoundedCornerShape(8.dp)),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                textColor = Color.White
//                textColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
        )

        //채팅 보내기 버튼
        IconButton(
            onClick = {
                if (message.isNotBlank()) {
                    onSendMessage(message)
                    message = ""
                }
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send Message",
                tint = Color.LightGray
            )
        }
    }
}
