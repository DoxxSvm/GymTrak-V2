package `in`.gym.trak.studio.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "approved" -> Color(0xFFE7F7F2) to Color(0xFF0D9488)
        "rejected" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        "pending" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(100.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            style = AppTextTheme.bold.copy(fontSize = 12.sp, color = textColor)
        )
    }
}
