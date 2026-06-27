package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import `in`.gym.trak.studio.theme.*

@Composable
expect fun TimePickerModal(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onClose: () -> Unit,
    onSelect: (Int, Int) -> Unit
)

fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val hStr = if (h < 10) "0$h" else "$h"
    val mStr = if (minute < 10) "0$minute" else "$minute"
    return "$hStr:$mStr $amPm"
}

/** Parses `"HH:mm"` (24h) and returns [formatTime] AM/PM (e.g. for plan batch_details). */
fun format24hStringToAmPm(time24: String): String {
    val parts = time24.trim().split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return formatTime(h, m)
}

/**
 * Normalizes plan shift times from the API (`"HH:mm"` or `"hh:mm AM/PM"`) to `"HH:mm"` for edit UI.
 */
fun normalizeTimeTo24hString(time: String): String {
    val t = time.trim()
    val upper = t.uppercase()
    if (!upper.contains("AM") && !upper.contains("PM")) {
        val parts = t.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }
    val isPm = upper.contains("PM")
    val isAm = upper.contains("AM")
    val core = t.replace(Regex("(?i)\\s*[AP]M\\s*"), "").trim()
    val parts = core.split(":")
    var h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    when {
        isPm && h != 12 -> h += 12
        isAm && h == 12 -> h = 0
    }
    h = h.coerceIn(0, 23)
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}
