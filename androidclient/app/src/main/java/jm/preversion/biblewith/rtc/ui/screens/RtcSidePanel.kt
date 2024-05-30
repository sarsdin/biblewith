package jm.preversion.biblewith.rtc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.sharp.Face
import androidx.compose.material.icons.sharp.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import jm.preversion.biblewith.rtc.RtcVm
import jm.preversion.biblewith.rtc.ui.theme.반투명검정

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpandableFAB(
    onSendFile : ()->Unit
) {
    val rtcVm = viewModel<RtcVm>()
    var expanded by remember { mutableStateOf(false) }
    var isShareScreen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 130.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
//        FloatingActionButton(
//            onClick = { expanded = !expanded },
//            icon = Icons.Default.Add,
//            backgroundColor = Color.Blue
//        )

        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .align(Alignment.BottomEnd)
//                .offset(y = (-60).dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(반투명검정)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Rounded.Add,
                contentDescription = if (expanded) "닫힘" else "열림",
                tint = Color.Yellow
            )
        }

        AnimatedVisibility(
            modifier = Modifier,
            enter = slideInHorizontally(initialOffsetX = { 200 }),
            exit = slideOutHorizontally(targetOffsetX = { 300 }),
            visible = expanded,
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.Green.copy(alpha = 0.7f), shape = CircleShape)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Sharp.Send,
                    contentDescription = "파일전송",
                    tint = Color.White,
                    modifier = Modifier.clickable {
                        onSendFile()
                        expanded = !expanded
                    }
                )
                Icon(
                    imageVector = Icons.Sharp.Face,
                    contentDescription = "화면공유",
                    modifier = Modifier.clickable {
                        isShareScreen = !isShareScreen
                        if (isShareScreen){
                            rtcVm.sessionManager.화면공유실행()
                        } else {
                            rtcVm.sessionManager.화면공유중지()
                        }
                        expanded = !expanded
                    },
                    tint = if (isShareScreen){
                        Color.Red
                    } else {
                        Color.White
                    }
                )
            }
        }
    }
}

@Composable
fun FloatingActionButton(onClick: () -> Unit, icon: ImageVector, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(backgroundColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = "FAB")
    }
}
