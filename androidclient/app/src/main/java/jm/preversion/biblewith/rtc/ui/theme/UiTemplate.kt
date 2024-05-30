package jm.preversion.biblewith.rtc.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object CustomModifier {

    fun rowModifier참가요청(): Modifier{
        return Modifier
            .fillMaxWidth(1f)
            .height(50.dp)
//                        .align(Alignment.Start)
            .background(color = Color(0xFF35898f))
    }

}