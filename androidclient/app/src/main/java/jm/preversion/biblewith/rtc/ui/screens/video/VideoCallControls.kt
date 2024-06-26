package jm.preversion.biblewith.rtc.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import jm.preversion.biblewith.R
import jm.preversion.biblewith.rtc.RtcVm
import jm.preversion.biblewith.rtc.ui.theme.반투명검정
import jm.preversion.biblewith.rtc.ui.theme.진초록

/**
 *  비디오 스크린의 컨트롤 패널을 다루는 컴포넌트.
 */
@Composable
fun VideoCallControls(
    modifier: Modifier,
    callMediaState: CallMediaState,
    actions: List<VideoCallControlAction> = buildDefaultCallControlActions(callMediaState = callMediaState),
    onCallAction: (CallAction) -> Unit
) {

    val rtcVm = viewModel<RtcVm>()

    val 요청자수 by rtcVm.방장에게접속요청자목록.collectAsState()

    LazyRow(
        modifier = modifier.padding(top = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(actions) { action ->

            // 요청자 아이콘의 처리. 요청이 있을때
            if ((action.callAction is CallAction.RequestList) && action.callAction.isRequest ){
                Box(
                    modifier = Modifier
                        .size(36.dp)
//                        .clip(CircleShape).clipToBounds()
                        .background(반투명검정, CircleShape) //action.background
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.Center)
                            //actions에서 받아온 VideoCallControlAction List의 현재 인덱스에 맞는 요소 안에서 CallAction 객체를 구현부에 전달해줌.
                            .clickable { onCallAction(action.callAction) },
                        tint = action.iconTint,
                        painter = action.icon,
                        contentDescription = null
                    )

                    // 요청자 숫자를 겹쳐진 아이콘으로 표시
                    Box(
                        modifier = Modifier
//                            .align(Alignment.BottomEnd)
                            .offset(x = 17.dp, y = 20.dp)
                            .size(18.dp)
                            .background(진초록, CircleShape)
                            .zIndex(1f)
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
//                            text = rtcVm.방장에게접속요청자목록size.toString(),
                            text = 요청자수.size().toString(),
                            style = TextStyle(
                                fontFamily = FontFamily(Font(R.font.binggraetaombold, FontWeight.Bold)),
                                fontSize = 9.sp,
                                color = Color.White
                            ),
                        )
                    }
                }


            //그외 나머지 (일반적)상황일때
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(반투명검정) //action.background
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.Center)
                            //actions에서 받아온 VideoCallControlAction List의 현재 인덱스에 맞는 요소 안에서 CallAction 객체를 구현부에 전달해줌.
                            .clickable { onCallAction(action.callAction) },
                        tint = action.iconTint,
                        painter = action.icon,
                        contentDescription = null
                    )
                }
            }

        }
    }
}
