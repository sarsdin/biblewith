/*
 * Copyright 2023 Stream.IO, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jm.preversion.biblewith.rtc.ui.screens.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jm.preversion.biblewith.rtc.webrtc.WebRTCSessionState

@Composable
fun StageScreen(
    state: WebRTCSessionState,
    onJoinCall: () -> Unit
) {



    Box(modifier = Modifier.fillMaxSize()) {
        //현재 상태가 상대방과 화상통화가 가능한지에 대한 여부를 기억.
        var enabledCall by remember { mutableStateOf(false) }
        //state안의 enum객체 값에 따라 텍스트값 변경
        val text = when (state) {
            WebRTCSessionState.Offline -> {
                enabledCall = false
//                stringResource(id = R.string.button_start_session)
                "세션 시작"
            }
            //웹소켓을 통해 시그널링 서버에 처음 접속하면 받는 시그널링 상태 명령
            //아직 원격 시그널링 서버에서 피어를 찾을 수 없는 상황.
            WebRTCSessionState.Impossible -> {
                enabledCall = false
//                stringResource(id = R.string.session_impossible)
                "피어가 한명 뿐임"
            }
            //시그널링 서버에서 피어를 찾은 상황. 자신 포함하여 2명 이상이면 서버에서
            // 웹소켓을 통해 'STATE Ready' 문자열을 보내고 뒤의 Ready를 짤라서 이곳
            //ui에서 상태값으로 활용됨.
            //그리고, 버튼을 활성화 시키고 세션 준비완료라고 텍스트 변경함.
            //이상태에서 버튼을 누르면(밑의 onJoinCall()) OFFER 명령메시지를 서버로 발송함.
            WebRTCSessionState.Ready -> {
                enabledCall = true
//                stringResource(id = R.string.session_ready)
                "세션 시작할 준비 완료"
            }
            //어떤 peer가 서버로 OFFER메시지를 보내면 Creating 메시지를 연결된 클라들에 보냄.
            WebRTCSessionState.Creating -> {
                enabledCall = true
//                stringResource(id = R.string.session_creating)
                "세션에 피어 참가 가능"
            }
            WebRTCSessionState.Active -> {
                enabledCall = false
//                stringResource(id = R.string.session_active)
                "세션에 피어 꽉참"
            }
        }


        // sessionManager.signalingClient.sessionStateFlow.collectAsState()의
        // 값에 따라 위의 문구가 바껴서 나오며, 클릭시
        Button(
            modifier = Modifier.align(Alignment.Center),
            //WebRTCSessionState 값에 따라 버튼의 활성화가 바뀜.
            //Ready상태가 되어야 버튼이 활성화됨.
            enabled = enabledCall,
            //이 컴포넌트함수를 실행할때 등록한 콜백을 실행함.
            onClick = { onJoinCall.invoke() }
        ) {
            Text(
                text = text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "seols_test",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5X
)
@Composable
fun PreviewStatgeScreen(){
    Box(modifier = Modifier.background(color = Color.White),
        contentAlignment = Alignment.TopCenter,

    ){
        Card(
            onClick = { /* Do something */ },
            modifier = Modifier.size(width = 220.dp, height = 100.dp),

        ) {
            Box(modifier = Modifier
                .fillMaxSize(1f)
                .padding(top = 10.dp)
                .background(color = Color.Green)) {
                Text("Clickable", Modifier.align(Alignment.Center)

                )

            }
        }
    }
}
