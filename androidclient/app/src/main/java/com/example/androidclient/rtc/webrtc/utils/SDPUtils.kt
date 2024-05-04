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

package com.example.androidclient.rtc.webrtc.utils

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend inline fun createValue(
    crossinline call: (SdpObserver) -> Unit
): Result<SessionDescription>

//함수에 = 로 코드를 할당할때 동작 방식 설명: '=' 에 할당된 코드는 이 함수(createValue)가 실행될때 사전 준비운동 같은 것임.
// 일단 suspendCoroutine이 동작하고 그 안의 내용들이 실행됨. 그리고, 옵져버 객체 생성 후, 이 함수의 매개변수인
// 함수 call을 실행함. 이때, 함수에 아까전 구현된 옵져버 객체를 인수로 넣어주고 실행함. 함수 call의 구현부는
// 이 함수(createValue())를 실행한 곳({})에 있음. call의 구현부에서 아까 넣어준 '옵져버 인터페이스의 구현부'가 실행되어
// resume() 메서드가 실행되면, 맨처음 실행한 suspendCoroutine의 동작이 마무리되고 createValue함수가 종료됨.
// 결국, = 에 할당된 것은 함수의 리턴값을 산출하는 코드가 됨.
= suspendCoroutine {
    val observer = object : SdpObserver {

        /**
         * Handling of create values.
         */
        override fun onCreateSuccess(description: SessionDescription?) {
            if (description != null) {
                it.resume(Result.success(description))
            } else {
                it.resume(Result.failure(RuntimeException("SessionDescription is null!")))
            }
        }

        override fun onCreateFailure(message: String?) =
            it.resume(Result.failure(RuntimeException(message)))

        /**
         * We ignore set results.
         */
        override fun onSetSuccess() = Unit
        override fun onSetFailure(p0: String?) = Unit
    }

    call(observer)
}


/**
 * 구현된 SdpObserver interface 객체를 이 함수의 파라메터에 '='을 이용해 할당해줌.
 */
suspend inline fun setValue(
    crossinline call: (SdpObserver) -> Unit
): Result<Unit> = suspendCoroutine {
    val observer = object : SdpObserver {
        /**
         * We ignore create results.
         */
        override fun onCreateFailure(p0: String?) = Unit
        override fun onCreateSuccess(p0: SessionDescription?) = Unit

        /**
         * Handling of set values.
         */
        override fun onSetSuccess() = it.resume(Result.success(Unit))
        override fun onSetFailure(message: String?) =
            it.resume(Result.failure(RuntimeException(message)))
    }

    call(observer)
}
