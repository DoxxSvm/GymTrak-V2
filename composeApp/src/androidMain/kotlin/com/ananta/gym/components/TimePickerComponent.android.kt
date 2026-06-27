package `in`.gym.trak.studio.components

import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun TimePickerModal(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onClose: () -> Unit,
    onSelect: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val pickerDialog = TimePickerDialog(
            context,
            { _, hour, minute -> 
                onSelect(hour, minute)
            },
            initialHour,
            initialMinute,
            false // is24Hour
        )
        pickerDialog.setMessage(title)
        pickerDialog.setOnDismissListener { 
            onClose() 
        }
        pickerDialog.show()
    }
}
