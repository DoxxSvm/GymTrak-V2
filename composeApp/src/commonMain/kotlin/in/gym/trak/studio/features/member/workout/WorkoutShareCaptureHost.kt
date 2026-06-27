package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import `in`.gym.trak.studio.utils.encodeToPngBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object WorkoutShareCaptureState {
    var capture: (suspend () -> ByteArray?)? = null
}

@Composable
internal fun NiceWorkoutShareCardCaptureHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()
    Box(
        modifier = modifier.drawWithContent {
            graphicsLayer.record {
                this@drawWithContent.drawContent()
            }
            drawContent()
        },
    ) {
        content()
    }
    DisposableEffect(graphicsLayer) {
        WorkoutShareCaptureState.capture = {
            withContext(Dispatchers.Main) {
                graphicsLayer.toImageBitmap().encodeToPngBytes()
            }
        }
        onDispose {
            WorkoutShareCaptureState.capture = null
        }
    }
}

internal suspend fun captureWorkoutShareCardImage(): ByteArray? {
    return WorkoutShareCaptureState.capture?.invoke()
}
