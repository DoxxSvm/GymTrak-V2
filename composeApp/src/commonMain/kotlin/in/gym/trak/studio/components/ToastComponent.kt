package `in`.gym.trak.studio.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.White
import kotlinx.coroutines.delay

@Composable
fun ToastComponent(
    message: String,
    onDismiss: () -> Unit,
    duration: Long = 3000L,
    isSuccess: Boolean = false
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(message) {
        visible = true
        delay(duration)
        visible = false
        delay(300) // wait for animation
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .background(
                        color = if (isSuccess) Color(0xFF166534).copy(alpha = 0.92f)
                        else Color(0xFFB91C1C).copy(alpha = 0.92f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message,
                    color = White,
                    style = AppTextTheme.medium.copy(fontSize = 14.sp),
                    maxLines = 3
                )
            }
        }
    }
}
