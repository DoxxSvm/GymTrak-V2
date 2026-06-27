package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun TimePickerModal(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onClose: () -> Unit,
    onSelect: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black),
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp)
                )

                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        timeSelectorSelectedContainerColor = Color(0xFFD1FAE5),
                        timeSelectorSelectedContentColor = Color(0xFF3C8C4A),
                        clockDialColor = Color(0xFFF8F9FA),
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = Color.Black,
                        selectorColor = PrimaryColor
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) {
                        Text("Cancel", style = AppTextTheme.bold.copy(color = Color.Gray))
                    }
                    TextButton(onClick = { onSelect(state.hour, state.minute) }) {
                        Text("OK", style = AppTextTheme.bold.copy(color = PrimaryColor))
                    }
                }
            }
        }
    }
}
