package com.example.androidclient.rtc.ui.screens

import androidx.compose.foundation.background
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
import com.example.androidclient.rtc.ui.theme.반투명검정
import com.example.androidclient.rtc.webrtc.sessions.ChatData
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
                    .padding(bottom = 56.dp),
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
                .offset(y = (-60).dp)
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
//        modifier = modifier
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
//            .fillMaxSize()
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
                delay(100) // 애니메이션을 위한 딜레이 추가
                listState.animateScrollToItem(chatMessages.size - 1) // 가장 최근 항목으로 스크롤
            }
        }
    }

}

@Composable
fun ChatItem(
//    modifier: Modifier = Modifier,
    chatData: ChatData) {
    Column(
//        modifier = modifier
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
