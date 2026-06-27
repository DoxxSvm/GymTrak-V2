package `in`.gym.trak.studio.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun GoogleMap(
    modifier: Modifier = Modifier,
    targetLocation: Pair<Double, Double>? = null,
    zoom: Float = 15f,
    tilt: Float = 0f,
    bearing: Float = 0f,
    onLocationUpdate: (lat: Double, lng: Double) -> Unit
)
