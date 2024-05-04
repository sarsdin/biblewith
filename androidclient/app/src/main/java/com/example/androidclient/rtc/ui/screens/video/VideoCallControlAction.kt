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

package com.example.androidclient.rtc.ui.screens.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.rtc.RtcVm
import com.example.androidclient.rtc.ui.theme.Disabled
import com.example.androidclient.rtc.ui.theme.Primary

/**
 * 액션 아이콘의 디자인 및 현재 동작정보를 담고있는 정보클래스.
 * (== Action Icon)
 */
data class VideoCallControlAction(
    val icon: Painter,
    val iconTint: Color,
    val background: Color,
    val callAction: CallAction
)

/**
 * 현재 통화화면에서 어떤 아이콘을 클릭해 동작을 하고 있는지와 그 상태를 저장하는 클래스.
 */
sealed class CallAction {
    data class RequestList(
        val isRequest: Boolean
    ) : CallAction()
    data class ToggleMicroPhone(
        val isEnabled: Boolean
    ) : CallAction()
    data class ToggleCamera(
        val isEnabled: Boolean
    ) : CallAction()
    data class Record(
        val isEnabled: Boolean
    ) : CallAction()
    object FlipCamera : CallAction()
    object LeaveCall : CallAction()
}

/**
 * 영상통화에서 사용할 액션 아이콘들의 초기화를 위해 관련 클래스를 만들고
 * 리스트로 묶어 리턴해주는 메서드.
 */
@Composable
fun buildDefaultCallControlActions(
    callMediaState: CallMediaState
): List<VideoCallControlAction> {

    val requestListIcon = painterResource(id = if (callMediaState.isRequestList) R.drawable.ic_userplus else R.drawable.ic_baseline_person_24)
    val microphoneIcon = painterResource(id = if (callMediaState.isMicrophoneEnabled) R.drawable.ic_mic_on else R.drawable.ic_mic_off)
    val cameraIcon = painterResource(id = if (callMediaState.isCameraEnabled) R.drawable.ic_videocam_on else R.drawable.ic_videocam_off)

    val rtcVm = viewModel<RtcVm>()

    val actions = mutableListOf<VideoCallControlAction>().apply {
        // 방장유무 조건에 따라 달라지는 요소 추가
        if (rtcVm.접속한방정보읽기["makerId"].asString == MyApp.userInfo.user_email) {
            add(VideoCallControlAction(
                icon = requestListIcon,
                iconTint = if (callMediaState.isRequestList) Color.Green else Color.LightGray,
                background = Primary,
                callAction = CallAction.RequestList(callMediaState.isRequestList)
            ))
        }

        // 공통 요소들 추가
        add(VideoCallControlAction(
            icon = microphoneIcon,
            iconTint = Color.White,
            background = Primary,
            callAction = CallAction.ToggleMicroPhone(callMediaState.isMicrophoneEnabled)
        ))
        add(VideoCallControlAction(
            icon = cameraIcon,
            iconTint = Color.White,
            background = Primary,
            callAction = CallAction.ToggleCamera(callMediaState.isCameraEnabled)
        ))
        add(VideoCallControlAction(
            icon = painterResource(id = R.drawable.ic_camera_flip),
            iconTint = Color.White,
            background = Primary,
            callAction = CallAction.FlipCamera
        ))
        add(VideoCallControlAction(
            icon = painterResource(id = R.drawable.baseline_fiber_manual_record_24),
            iconTint = if (callMediaState.isRecordEnabled) Color.Red else Color.LightGray,
            background = Primary,
            callAction = CallAction.Record(callMediaState.isRecordEnabled)
        ))
        add(VideoCallControlAction(
            icon = painterResource(id = R.drawable.ic_call_end),
            iconTint = Disabled,
            background = Primary,
            callAction = CallAction.LeaveCall
        ))
    }

    return actions.toList()

}
