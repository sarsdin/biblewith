package jm.preversion.biblewith.rtc.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import jm.preversion.biblewith.R
import jm.preversion.biblewith.rtc.RtcVm
import jm.preversion.biblewith.rtc.ui.theme.CustomModifier
import jm.preversion.biblewith.rtc.webrtc.StandardCommand


val padding = 16.dp

@Composable
fun JoinRequestDialog(onDismiss: () -> Unit) {
    val contextForToast = LocalContext.current.applicationContext
    val rtcVm = viewModel<RtcVm>()
    val _requestList by rtcVm.방장에게접속요청자목록.collectAsState()
    //서버로부터 받아온 요청자목록을 List로 변환하여, view에 사용.
    val requestList = _requestList.run {
        map {
            it.asJsonObject
        }
    }


    Dialog(
        onDismissRequest = {
            onDismiss()
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = CustomModifier.rowModifier참가요청(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier
                            .padding(top = 0.dp, bottom = 0.dp, end = 0.dp, start = 0.dp),
                        painter = painterResource(id = R.drawable.ic_userplus),
                        contentDescription = "참가 요청",
                        colorFilter = ColorFilter.tint(color = Color.White,),
                    )

                    Spacer(Modifier.size(padding))

                    Text(
                        modifier = Modifier/*.padding(top = 16.dp, bottom = 16.dp)*/,
                        text = "참가요청",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily(Font(R.font.melonabold, FontWeight.Bold)),
                            fontSize = 20.sp
                        )
                    )
                }

                // todo 이부분 요청자 리스트로 구성. 요청을 하면 방장이 리스트를 확인가능해야함.
                //  방장이 거부하거나, 요청자가 취소하면 실시간으로 소켓통신하여 리스트에서 제거해야함.
                //  허용시 리스트에서 제거 후, 서버로 그 요청자에게 방에 참가하라는 신호를 보내야함.
                //  리스트는 서버의 Room객체에 개별 List로 담아야 할듯.
                LazyColumn(
                    modifier = Modifier.heightIn(0.dp, 350.dp),
                    userScrollEnabled = true
                ){
                    itemsIndexed(requestList) { i, item ->
//                    items(2) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .height(56.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                                text = item["nick"].asString,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    fontFamily = FontFamily(Font(R.font.binggraetaombold, FontWeight.Bold)),
                                    fontSize = 16.sp
                                )
                            )

                            Row(
                                modifier = Modifier
//                                    .size(width = 100.dp, height = 50.dp),
//                                    .fillMaxWidth(1f)
                                    .height(50.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        // todo 여기서 수락시 서버로 해당하는 접속자에게 방에 참가하라는 명령을 보내줌.
                                        rtcVm.sessionManager.signalingClient.sendCommand(
                                            StandardCommand.방참가수락,
                                            item.run {
                                                deepCopy().also {
                                                    it.addProperty("command", StandardCommand.방참가수락.name)
                                                }
                                            }
                                        )


//                                    onDismiss()
                                        Toast.makeText(
                                            contextForToast,
                                            "Click: 수락",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }) {
                                    Text(
                                        text = "수락",
                                        color = Color(0xFF35898f),
                                        style = TextStyle(
                                            fontFamily = FontFamily(
                                                Font(
                                                    R.font.binggraetaom,
                                                    FontWeight.Normal
                                                )
                                            ),
                                            fontSize = 14.sp
                                        )
                                    )
                                }

                                TextButton(
                                    onClick = {


//                                    onDismiss()
                                        Toast.makeText(
                                            contextForToast,
                                            "Click: 거부",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }) {
                                    Text(
                                        text = "거부",
                                        color = Color(0xFF35898f),
                                        style = TextStyle(
                                            fontFamily = FontFamily(
                                                Font(
                                                    R.font.binggraetaom,
                                                    FontWeight.Normal
                                                )
                                            ),
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                            }



                        } // Row end

                    } // itemsIndexed end
                } // LazyColumn end



                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF35898f)),
                    onClick = {
                        onDismiss()
                        Toast.makeText(
                            contextForToast,
                            "Click: 닫기",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                    Text(
                        text = "닫기",
                        color = Color.White,
                        style = TextStyle(
                            fontFamily = FontFamily(
                                Font(
                                    R.font.binggraetaom,
                                    FontWeight.Medium
                                )
                            ),
                            fontSize = 16.sp
                        )
                    )
                }

//                TextButton(
//                    onClick = {
//                        onDismiss()
//                        Toast.makeText(
//                            contextForToast,
//                            "Click: 투명버튼",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }) {
//                    Text(
//                        text = "투명버튼",
//                        color = Color(0xFF35898f),
//                        style = TextStyle(
//                            fontFamily = FontFamily(
//                                Font(
//                                    R.font.binggraetaom,
//                                    FontWeight.Normal
//                                )
//                            ),
//                            fontSize = 14.sp
//                        )
//                    )
//                }

            }
        } //Surface End
    } //Dialog End
}


@Preview(
    name = "seols_test",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5X
)
@Composable
fun JoinRequestDialog_pr() {
//    val contextForToast = LocalContext.current.applicationContext

//    JoinRequestDialog{}

//    Dialog(
//        onDismissRequest = {
//        }
//    ) {
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth(1f),
//            elevation = 4.dp
//        ) {
//            Column(
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth(1f)
//                        .height(50.dp)
////                        .align(Alignment.Start)
//                        .background(color = Color(0xFF35898f)),
//                    horizontalArrangement = Arrangement.Center,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Image(
//                        modifier = Modifier
//                            .padding(top = 0.dp, bottom = 0.dp, end = 0.dp, start = 0.dp),
//                        painter = painterResource(id = R.drawable.ic_userplus),
//                        contentDescription = "2-Step Verification",
////                        alignment = Alignment.Center
//                    )
//                    Spacer(Modifier.size(padding))
//                    Text(
//                        modifier = Modifier/*.padding(top = 16.dp, bottom = 16.dp)*/,
//                        text = "참가요청",
//                        textAlign = TextAlign.Center,
//                        style = TextStyle(
//                            fontFamily = FontFamily(Font(R.font.melonabold, FontWeight.Bold)),
//                            fontSize = 20.sp
//                        )
//                    )
//                }
//
//                Text(
//                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
//                    text = "아무개1",
//                    textAlign = TextAlign.Center,
//                    style = TextStyle(
//                        fontFamily = FontFamily(Font(R.font.binggraetaombold, FontWeight.Bold)),
//                        fontSize = 16.sp
//                    )
//                )
//
//
//
//                Button(
//                    modifier = Modifier
//                        .fillMaxWidth(1f)
//                        .padding(top = 36.dp, start = 36.dp, end = 36.dp, bottom = 8.dp),
//                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF35898f)),
//                    onClick = {
//                    }) {
//                    Text(
//                        text = "Setup Now",
//                        color = Color.White,
//                        style = TextStyle(
//                            fontFamily = FontFamily(
//                                Font(
//                                    R.font.binggraetaom,
//                                    FontWeight.Medium
//                                )
//                            ),
//                            fontSize = 16.sp
//                        )
//                    )
//                }
//
//                TextButton(
//                    onClick = {
//                    }) {
//                    Text(
//                        text = "I'll Do It Later",
//                        color = Color(0xFF35898f),
//                        style = TextStyle(
//                            fontFamily = FontFamily(
//                                Font(
//                                    R.font.binggraetaom,
//                                    FontWeight.Normal
//                                )
//                            ),
//                            fontSize = 14.sp
//                        )
//                    )
//                }
//            }
//        } //Surface End
//    } //Dialog End
}


//@Preview(
//    name = "seols_test",
//    showSystemUi = true,
//    showBackground = true,
//    device = Devices.NEXUS_5X
//)
//@Composable
//fun JoinRequestDialog_pr() {
////    val contextForToast = LocalContext.current.applicationContext
//
//    Dialog(
//        onDismissRequest = {
//        }
//    ) {
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth(1f),
//            elevation = 4.dp
//        ) {
//            Column(
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth(1f)
//                        .height(50.dp)
//                        .background(color = Color(0xFF35898f)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Image(
//                        modifier = Modifier
//                            .padding(top = 16.dp, bottom = 16.dp, end = 0.dp, start = 0.dp),
//                        painter = painterResource(id = R.drawable.heart),
//                        contentDescription = "2-Step Verification",
//                        alignment = Alignment.Center
//                    )
//                }
//
//                Text(
//                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
//                    text = "2-Step Verification",
//                    textAlign = TextAlign.Center,
//                    style = TextStyle(
//                        fontFamily = FontFamily(Font(R.font.binggraetaombold, FontWeight.Bold)),
//                        fontSize = 20.sp
//                    )
//                )
//
//                Text(
//                    modifier = Modifier.padding(start = 12.dp, end = 12.dp),
//                    text = "Setup 2-Step Verification to add additional layer of security to your account.",
//                    textAlign = TextAlign.Center,
//                    style = TextStyle(
//                        fontFamily = FontFamily(Font(R.font.binggraetaom, FontWeight.Normal)),
//                        fontSize = 14.sp
//                    )
//                )
//
//                Button(
//                    modifier = Modifier
//                        .fillMaxWidth(1f)
//                        .padding(top = 36.dp, start = 36.dp, end = 36.dp, bottom = 8.dp),
//                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF35898f)),
//                    onClick = {
//                    }) {
//                    Text(
//                        text = "Setup Now",
//                        color = Color.White,
//                        style = TextStyle(
//                            fontFamily = FontFamily(
//                                Font(
//                                    R.font.binggraetaom,
//                                    FontWeight.Medium
//                                )
//                            ),
//                            fontSize = 16.sp
//                        )
//                    )
//                }
//
//                TextButton(
//                    onClick = {
//                    }) {
//                    Text(
//                        text = "I'll Do It Later",
//                        color = Color(0xFF35898f),
//                        style = TextStyle(
//                            fontFamily = FontFamily(
//                                Font(
//                                    R.font.binggraetaom,
//                                    FontWeight.Normal
//                                )
//                            ),
//                            fontSize = 14.sp
//                        )
//                    )
//                }
//            }
//        } //Surface End
//    } //Dialog End
//}
