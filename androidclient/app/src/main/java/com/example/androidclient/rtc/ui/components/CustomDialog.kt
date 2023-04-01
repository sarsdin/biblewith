package com.example.androidclient.rtc.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.example.androidclient.rtc.RtcFm
import com.google.gson.JsonObject

@Composable
fun CustomDialog(
    onConfirmClick: (JsonObject) -> Unit,
    onDismissClick: () -> Unit,
) {
//    var title = title
//    var size = size
//    var pwd = pwd
//    var title: String = ""
//    var size: Int = 4
//    var pwd: String = ""
    var title by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("4") }
    var pwd by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismissClick() },
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
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            size = input
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
                    onValueChange = { pwd = it }, // 상태 변수 업데이트
                    label = { Text("비밀번호") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        // 클릭시 방 만들기: 소켓으로 서버에 '방만들기' 명령내림.
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmClick(
                        //상위 전달 인자로 json object를 전달함.
                        JsonObject().apply { 
                            addProperty("title", title)
                            addProperty("size", size)
                            addProperty("pwd", pwd)
                        }
                    )
                    onDismissClick()
                }
            ) {
                Text("확인")
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