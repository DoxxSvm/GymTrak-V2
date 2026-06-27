package `in`.gym.trak.studio.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.gym.trak.studio.theme.PrimaryColor

/**
 * Reusable loading / progress indicator for the app.
 * Use as overlay or inline when loading data.
 */
@Composable
fun CommonProgress(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
        modifier = modifier.size(48.dp),
        color = PrimaryColor,
        strokeWidth = 3.dp
    )
}

/**
 * Full-screen centered loading overlay.
 */
@Composable
fun CommonProgressOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CommonProgress()
    }
}
