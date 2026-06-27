package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.gym.trak.studio.network.NetworkMonitor
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.White
import kotlinx.coroutines.delay

@Composable
fun NoInternetDialog() {
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000) // Recheck every 3 seconds
            NetworkMonitor.recheck()
        }
    }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(White, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No internet connection. Please enable internet.",
                    style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
