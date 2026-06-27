package `in`.gym.trak.studio.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black

/**
 * Common header for authentication and onboarding screens.
 */
@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    textAlign: TextAlign = TextAlign.Start
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = title,
            style = AppTextTheme.semiBold.copy(
                fontSize = 24.sp,
                color = Color.Black
            ),
            textAlign = textAlign
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = AppTextTheme.regular.copy(
                fontSize = 12.sp,
                color = Black.copy(alpha = 0.80f)
            ),
            textAlign = textAlign
        )
    }
}
