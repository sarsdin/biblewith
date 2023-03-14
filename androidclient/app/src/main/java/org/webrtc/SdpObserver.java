/*
 *  Copyright 2013 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

/** Interface for observing SDP-related events.
 *  SdpObserver는 webrtc ndk lib 에 전달하기 위한 용도로 사용된다.
 *  SdpObserver객체를 생성하여 SDPUtils.kt 내의 createValue()메소드를 통해 ndk에 인터페이스 객체와 sdp객체를 전달하면
 *  자바쪽에서 구현한 SdpObserver 인터페이스의 구현메소드를 ndk 쪽에서 실행할 수 있게된다.
 * */
public interface SdpObserver {
  /** Called on success of Create{Offer,Answer}(). */
  @CalledByNative
  void onCreateSuccess(SessionDescription sdp);

  /** Called on success of Set{Local,Remote}Description(). */
  @CalledByNative
  void onSetSuccess();

  /** Called on error of Create{Offer,Answer}(). */
  @CalledByNative
  void onCreateFailure(String error);

  /** Called on error of Set{Local,Remote}Description(). */
  @CalledByNative
  void onSetFailure(String error);
}
