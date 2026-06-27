package `in`.gym.trak.studio.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black

/**
 * Consistent section label / field label used across form screens.
 */
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
    )
}
